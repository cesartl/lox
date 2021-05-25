package com.ctl.klox

import org.junit.jupiter.api.Test

class ScriptDebug {

    val script = """
        class Doughnut {
          cook() {
            print "Fry until golden brown.";
          }
        }

        class BostonCream < Doughnut {
          cook() {
            super.cook();
            print "Pipe full of custard and coat with chocolate.";
          }
        }

        BostonCream().cook();
    """.trimIndent()

    @Test
    internal fun debugScript() {
      Lox.debug(script)
    }
}