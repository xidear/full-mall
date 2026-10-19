package com.fullmall.supplier.app;

import com.fullmall.platform.importing.CsvImportException;
import com.fullmall.platform.importing.CsvRowError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupplierStockFeedCsvParserTest {

    private final SupplierStockFeedCsvParser parser = new SupplierStockFeedCsvParser();

    private static final String HEADER =
            "skuId,supplierSkuCode,availableQuantity,priceAmount,priceCurrency\n";

    @Test
    void parsesValidRows() {
        var rows = parser.parse(HEADER
                + "11111111-1111-1111-1111-111111111111,SUP-1,10,3500,CNY\n"
                + "22222222-2222-2222-2222-222222222222,SUP-2,0,,cny\n");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).availableQuantity()).isEqualTo(10);
        assertThat(rows.get(0).priceAmount()).isEqualTo(3500);
        assertThat(rows.get(1).priceAmount()).isNull();
        assertThat(rows.get(1).priceCurrency()).isNull();
    }

    @Test
    void rejectsWrongHeader() {
        assertThatThrownBy(() -> parser.parse("a,b,c\n1,2,3\n"))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("表头");
    }

    @Test
    void collectsAllRowErrorsInsteadOfStoppingAtFirst() {
        assertThatThrownBy(() -> parser.parse(HEADER
                + "not-a-uuid,SUP-1,10,3500,CNY\n"
                + "11111111-1111-1111-1111-111111111111,, -3,abc,CN\n"))
                .isInstanceOfSatisfying(CsvImportException.class, error -> {
                    // 4 处问题：uuid 非法、skuCode 空、数量为负、价格不是整数
                    assertThat(error.errors()).hasSize(4);
                    assertThat(error.errors()).anySatisfy(e ->
                            assertThat(e.field()).isEqualTo("skuId"));
                    assertThat(error.errors()).anySatisfy(e ->
                            assertThat(e.field()).isEqualTo("availableQuantity"));
                    assertThat(error.errors()).anySatisfy(e ->
                            assertThat(e.field()).isEqualTo("priceAmount"));
                });
    }

    @Test
    void rejectsDuplicateSkuIdWithinFile() {
        String repeated = "11111111-1111-1111-1111-111111111111";

        assertThatThrownBy(() -> parser.parse(HEADER
                + repeated + ",SUP-1,5,100,CNY\n"
                + repeated + ",SUP-2,6,100,CNY\n"))
                .isInstanceOfSatisfying(CsvImportException.class, error ->
                        assertThat(error.errors()).anySatisfy(e ->
                                assertThat(e.reason()).contains("重复")));
    }

    @Test
    void rejectsEmptyFileAndHeaderOnlyFile() {
        assertThatThrownBy(() -> parser.parse("  "))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("为空");
        assertThatThrownBy(() -> parser.parse(HEADER))
                .isInstanceOf(CsvImportException.class)
                .hasMessageContaining("没有数据行");
    }

    @Test
    void rowNumbersPointAtRealCsvLines() {
        assertThatThrownBy(() -> parser.parse(HEADER
                + "11111111-1111-1111-1111-111111111111,SUP-1,1,100,CNY\n"
                + "bad-uuid,SUP-2,1,100,CNY\n"))
                .isInstanceOfSatisfying(CsvImportException.class, error ->
                        assertThat(error.errors()).extracting(CsvRowError::rowNumber).contains(3));
    }
}