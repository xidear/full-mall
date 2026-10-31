package com.fullmall.supplier;

import com.fullmall.exchange.app.ExchangeRateService;
import com.fullmall.platform.audit.AuditEventService;
import com.fullmall.platform.error.ApiException;
import com.fullmall.supplier.app.SupplierReturnService;
import com.fullmall.supplier.app.SupplierSettlementService;
import com.fullmall.supplier.infra.SupplierSettlementRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 供应商结算：多币种按汇率折算到基准币种；缺汇率明确失败，不按 1:1 兜底。 */
class SupplierSettlementMultiCurrencyTest {

    private static final Instant START = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-01T00:00:00Z");
    private static final UUID SUPPLIER = UUID.randomUUID();

    private final SupplierSettlementRepository repository = mock(SupplierSettlementRepository.class);
    private final SupplierReturnService returns = mock(SupplierReturnService.class);
    private final ExchangeRateService rates = mock(ExchangeRateService.class);
    private final SupplierSettlementService service = new SupplierSettlementService(
            repository, mock(AuditEventService.class), new BigDecimal("0.10"), returns, rates, "CNY");

    @Test
    void multiCurrencyAggregatesAreConvertedToBaseCurrency() {
        when(repository.aggregate(any(), any(), any())).thenReturn(List.of(
                new SupplierSettlementRepository.PeriodAggregate("USD", 2, 10_000),
                new SupplierSettlementRepository.PeriodAggregate("CNY", 1, 72_000)));
        when(returns.settledAmount(any(), any(), any(), anyString())).thenReturn(0L);
        when(rates.convert(anyLong(), anyString(), anyString(), any())).thenAnswer(invocation -> {
            long amount = invocation.getArgument(0);
            String from = invocation.getArgument(1);
            return "USD".equals(from)
                    ? conversion(amount, "USD", Math.round(amount * 7.2), "CNY", "7.2")
                    : conversion(amount, "CNY", amount, "CNY", "1");
        });
        AtomicReference<UUID> insertedId = new AtomicReference<>();
        doAnswer(invocation -> {
            insertedId.set(invocation.getArgument(0));
            return null;
        }).when(repository).insert(any(), any(), any(), any(), anyInt(), anyLong(), anyLong(),
                anyLong(), anyLong(), anyString(), anyString());
        when(repository.findBySupplier(any(), anyInt())).thenAnswer(invocation -> List.of(
                new SupplierSettlementRepository.SettlementRow(
                        insertedId.get(), SUPPLIER, START, END, 3,
                        144_000, 0, 14_400, 129_600, "CNY", "DRAFT", Instant.now())));

        var row = service.generate(SUPPLIER, START, END, "admin");

        assertThat(row.currency()).isEqualTo("CNY");
        verify(repository).insert(any(), eq(SUPPLIER), eq(START), eq(END), eq(3),
                eq(144_000L), eq(0L), eq(14_400L), eq(129_600L), eq("CNY"), eq("DRAFT"));
    }

    @Test
    void missingRateFailsExplicitly() {
        when(repository.aggregate(any(), any(), any())).thenReturn(List.of(
                new SupplierSettlementRepository.PeriodAggregate("USD", 1, 10_000)));
        when(returns.settledAmount(any(), any(), any(), anyString())).thenReturn(0L);
        when(rates.convert(anyLong(), anyString(), anyString(), any()))
                .thenThrow(ApiException.conflict("EXCHANGE_RATE_NOT_FOUND", "no rate"));

        assertThatThrownBy(() -> service.generate(SUPPLIER, START, END, "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> assertThat(((ApiException) error).code())
                        .isEqualTo("SETTLEMENT_FX_RATE_MISSING"));
    }

    private static ExchangeRateService.Conversion conversion(long amount, String from, long converted,
                                                             String to, String rate) {
        return new ExchangeRateService.Conversion(amount, from, converted, to,
                new BigDecimal(rate), END, false,
                com.fullmall.platform.money.CurrencyPrecision.minorUnits(from),
                com.fullmall.platform.money.CurrencyPrecision.minorUnits(to));
    }
}