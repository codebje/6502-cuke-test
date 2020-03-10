package com.github.codebje.steps;

import com.github.codebje.cc65debug.DebugInfo;
import com.github.codebje.devices.Memory;
import com.github.codebje.exceptions.MemoryAccessException;
import com.github.codebje.machines.Machine;
import com.github.codebje.machines.HeadlessVeronica;
import com.google.common.base.Charsets;
import cucumber.api.java8.En;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import java.io.File;

public class JsrStepDefinitions implements En {
    private final static Logger logger = LoggerFactory.getLogger(JsrStepDefinitions.class);

    private Machine machine;
    private DebugInfo debugInfo = DebugInfo.emptyDebugInfo();

    public JsrStepDefinitions() throws Exception {

        machine = new HeadlessVeronica();

        Given("^I have a ROM file named \"(.*)\"$", (String rom) -> {

            File file = new File(rom);

            long size = file.length();
            if (size != machine.getRomSize()) {
                throw new RuntimeException("ROM file must be exactly " + String.valueOf(machine.getRomSize()) + " bytes.");
            }

            try {

                Memory mem = Memory.makeROM(machine.getRomBase(), machine.getRomBase() + machine.getRomSize() - 1, file);
                machine.setRom(mem);
                machine.getCpu().reset();

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        });

        Given("^I have a debug file named \"(.*)\"$", (String info) -> {

            try {
                debugInfo = DebugInfo.loadDebugFile(new File(info));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        });

        Given("^the accumulator is (\\d+)$", (Integer areg) -> {
            machine.getCpu().setAccumulator(areg);
        });

        Given("^byte (\\d+) is stored at location (\\d+)$", (Integer val, Integer loc) -> {
            try {
                machine.getRam().write(loc, val);
            } catch (MemoryAccessException ex) {
                throw new RuntimeException(ex);
            }
        });

        Given("^an empty program is loaded$", () -> {
            try {
                Integer clear = debugInfo.getSymbolAddress("clear");
                if (clear == null)
                    throw new MemoryAccessException("undefined symbol: clear");

                Integer restart = debugInfo.getSymbolAddress("restart");
                if (restart == null)
                    throw new MemoryAccessException("undefined symbol: restart");

                machine.getCpu().setProgramCounter(clear);
                while (machine.getCpu().getProgramCounter() < restart) {
                    machine.getCpu().step();
                }

            } catch (MemoryAccessException ex) {
                throw new RuntimeException(ex);
            }
        });

        Given("^memory at (.+) contains$", (String location, String contents) -> {
            try {
                Integer target = debugInfo.getSymbolAddress(location);
                if (target == null)
                    throw new MemoryAccessException("undefined symbol: " + location);

                // Join all the lines together
                contents = contents.replace("\n", "");

                // Unescape Java escape sequences
                contents = StringEscapeUtils.unescapeJava(contents);

                int address = target;
                for (byte b : contents.getBytes(Charsets.UTF_8)) {
                    machine.getBus().write(address, b);
                    address++;
                }

            } catch (MemoryAccessException ex) {
                throw new RuntimeException(ex);
            }
        });


        Given("^parameter one is (\\d+)$", (Integer arg1) -> {
            try {
                machine.getRam().write(0, arg1 & 0xff);
                machine.getRam().write(1, arg1 >> 8);
            } catch (MemoryAccessException ex) {
                throw new RuntimeException(ex);
            }
        });

        When("^I call subroutine \"([^\"]*)\"$", (String label) -> {

            Integer addr = debugInfo.getSymbolAddress(label);

            if (addr == null) {
                throw new RuntimeException("Symbol " + label + " is not defined");
            }

            machine.getCpu().setProgramCounter(addr);

            try {

                machine.getCpu().stackPush(0xff);
                machine.getCpu().stackPush(0xfe);

                while (machine.getCpu().getProgramCounter() != 0xffff) {

                    System.out.println(String.format("%04x %-20s %02x%02x %c%c",
                            machine.getCpu().getProgramCounter(),
                            machine.getCpu().disassembleNextOp(),
                            machine.getBus().read(0x05, false),
                            machine.getBus().read(0x04, false),
                            machine.getCpu().getCarryFlag() ? 'C' : 'c',
                            machine.getCpu().getZeroFlag() ? 'Z' : 'z'
                            ));
                    machine.getCpu().step();

                }

            } catch (MemoryAccessException ex) {
                throw new RuntimeException(ex);
            }

        });

        Then("^the accumulator should be (\\d+)$", (Integer acc) -> {
            assertEquals((int)acc, machine.getCpu().getAccumulator());
        });

        Then("^the carry flag should be (SET|CLEAR)$", (String carry) -> {
           if (carry.equals("SET"))
               assertTrue("the carry flag is CLEAR", machine.getCpu().getCarryFlag());
           else
               assertFalse("the carry flag is SET", machine.getCpu().getCarryFlag());
        });

        Then("^the zero flag should be (SET|CLEAR)$", (String zero) -> {
            if (zero.equals("SET"))
                assertTrue("the zero flag is CLEAR", machine.getCpu().getZeroFlag());
            else
                assertFalse("the zero flag is SET", machine.getCpu().getZeroFlag());
        });

        Then("^the return value should be (\\d+)$", (Integer expected) -> {
            try {

                int val = machine.getBus().read(0x4, false)
                        + (machine.getBus().read(0x5, false) << 8);

                assertEquals((int)expected, val);

            } catch (MemoryAccessException ex) {
                throw new RuntimeException(ex);
            }
        });

    }
}
