/**
 * e2e 数据清理：只删 **199 前缀**（e2e 专用号段）的测试会员，且跳过有资金/交易痕迹的账号。
 *
 * <p>清理顺序（实测过 FK）：先删"非资金"子行（会话/收藏/足迹/购物车），
 * 再尝试删会员；若仍被资金/交易类子表挡住（订单、积分流水、券、储值、砍价、评价、POS），
 * 就保留该会员并计数上报。测试收尾不应该去删资金流水——那是财务记录。
 *
 * <p>清不掉的会员数量必须打印出来（不静默跳过）；要彻底回出厂数据用 make db-reset。
 */
import { execFileSync } from 'node:child_process'

const CONTAINER = process.env.E2E_DB_CONTAINER ?? 'fullmall-postgres-1'
const DB = process.env.E2E_DB_NAME ?? 'fullmall'
const USER = process.env.E2E_DB_USER ?? 'fullmall'

const sql = `
DO $$
DECLARE
  r record;
  deleted int := 0;
  kept int := 0;
BEGIN
  FOR r IN SELECT id FROM member WHERE phone LIKE '199%' LOOP
    -- 非资金类明细，删干净不影响对账
    DELETE FROM member_session WHERE member_id = r.id;
    DELETE FROM member_favorite WHERE member_id = r.id;
    DELETE FROM member_browse_history WHERE member_id = r.id;
    BEGIN
      DELETE FROM member WHERE id = r.id;
      deleted := deleted + 1;
    EXCEPTION WHEN foreign_key_violation THEN
      kept := kept + 1;
    END;
  END LOOP;
  RAISE NOTICE 'e2e members deleted=%, kept(has orders/ledger)=%', deleted, kept;
END $$;

SELECT (SELECT count(*) FROM member WHERE phone LIKE '199%') AS remaining;
`

export default async function globalTeardown(): Promise<void> {
  try {
    const output = execFileSync('docker', [
      'exec', CONTAINER, 'psql', '-U', USER, '-d', DB, '-t', '-A', '-c', sql
    ], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] })
    const remaining = output.trim().split('\n').filter(Boolean).pop()
    console.log('[e2e teardown] 剩余 199 前缀会员（有订单/流水，按策略保留）：' + remaining)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    console.error('[e2e teardown] 清理失败，数据未清理：' + message)
    process.exitCode = 1
  }
}
