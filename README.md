# 6502 test harness

This abandoned project will test 6502 binaries according to a Cucumber step definition, making use of the [VirtualVeronica](https://github.com/codebje/VirtualVeronica) as the emulation system.

A test case looks somewhat like this:

```
Feature: load a rom file

    Background:
        Given I have a ROM file named "../vbasic/veronica.rom"
        And I have a debug file named "../vbasic/veronica.dbg"
        And an empty program is loaded

    Scenario: empty program
        Given parameter one is 10
        When I call subroutine "findLine"
        Then the zero flag should be CLEAR
        And the return value should be 573

    Scenario: insert between lines
        Given memory at PROGRAM contains
            """
            \u000a\u0000\u0010print"line 10"
            \u0064\u0000\u0011print"line 100"
            """
        And parameter one is 25
        When I call subroutine "findLine"
        Then the zero flag should be CLEAR
        And the return value should be 589
```

If you want to make use of the project, you'll probably need to (a) fix up the VirtualVeronica according to your machine's hardware, and (b) define steps to control what peripherals present as.
