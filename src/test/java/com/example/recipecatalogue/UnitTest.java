package com.example.recipecatalogue;

import com.example.recipecatalogue.Model.Unit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitTest {

    @Test
    void parseIsLenient() {
        assertThat(Unit.parse("g")).isEqualTo(Unit.G);
        assertThat(Unit.parse(" Grams ")).isEqualTo(Unit.G);
        assertThat(Unit.parse("TBSP")).isEqualTo(Unit.TBSP);
        assertThat(Unit.parse("tablespoon")).isEqualTo(Unit.TBSP);
        assertThat(Unit.parse("cup")).isEqualTo(Unit.CUP);
        assertThat(Unit.parse("fl_oz")).isEqualTo(Unit.FL_OZ);
    }

    @Test
    void parseRejectsUnknown() {
        assertThatThrownBy(() -> Unit.parse("smidgen"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("smidgen");
    }

    @Test
    void toBaseConvertsToGramsOrMillilitres() {
        assertThat(Unit.KG.toBase(1.5)).isEqualTo(1500.0);          // -> grams
        assertThat(Unit.OZ.toBase(1)).isCloseTo(28.3495, org.assertj.core.data.Offset.offset(1e-3));
        assertThat(Unit.L.toBase(2)).isEqualTo(2000.0);             // -> millilitres
        assertThat(Unit.TBSP.toBase(1)).isCloseTo(14.7868, org.assertj.core.data.Offset.offset(1e-3));
    }

    @Test
    void dimensionsAreClassified() {
        assertThat(Unit.G.dimension()).isEqualTo(Unit.Dimension.MASS);
        assertThat(Unit.CUP.dimension()).isEqualTo(Unit.Dimension.VOLUME);
        assertThat(Unit.CLOVE.dimension()).isEqualTo(Unit.Dimension.COUNT);
    }
}
