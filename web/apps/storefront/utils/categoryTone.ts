const tones: Record<string, string> = {
  '99999999-9999-9999-9999-999999999999': 'tone-fresh',
  '99999999-9999-9999-9999-999999999998': 'tone-home',
  '99999999-9999-9999-9999-999999999997': 'tone-digital',
  '99999999-9999-9999-9999-999999999996': 'tone-industrial',
  '99999999-9999-9999-9999-999999999995': 'tone-beauty',
  '99999999-9999-9999-9999-999999999994': 'tone-sports'
}

export function categoryTone(categoryId?: string) {
  return tones[categoryId ?? ''] ?? 'tone-home'
}