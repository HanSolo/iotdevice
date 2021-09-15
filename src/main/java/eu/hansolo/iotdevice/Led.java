/*
 * Copyright (c) 2021 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.iotdevice;

import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalOutputProvider;
import com.pi4j.io.gpio.digital.DigitalState;
import eu.hansolo.iotmodules.actors.RgbLed;
import eu.hansolo.iotmodules.tools.Helper;


public class Led extends RgbLed {
    private Context                    pi4j;
    private DigitalOutputConfigBuilder redConfig;
    private DigitalOutputConfigBuilder greenConfig;
    private DigitalOutputConfigBuilder blueConfig;
    private DigitalOutput              red;
    private DigitalOutput              green;
    private DigitalOutput              blue;


    public Led(final String id, final Context pi4j, final int redPin, final int greenPin, final int bluePin) {
        super(id);

        this.pi4j = pi4j;

        this.redConfig = DigitalOutput.newConfigBuilder(pi4j)
                                      .id("red")
                                      .name("RED")
                                      .address(redPin)
                                      .shutdown(DigitalState.LOW)
                                      .initial(DigitalState.LOW)
                                      .provider(DigitalOutputProvider.class);

        this.greenConfig = DigitalOutput.newConfigBuilder(pi4j)
                                        .id("green")
                                        .name("GREEN")
                                        .address(greenPin)
                                        .shutdown(DigitalState.LOW)
                                        .initial(DigitalState.LOW)
                                        .provider(DigitalOutputProvider.class);

        this.blueConfig = DigitalOutput.newConfigBuilder(pi4j)
                                       .id("blue")
                                       .name("BLUE")
                                       .address(bluePin)
                                       .shutdown(DigitalState.LOW)
                                       .initial(DigitalState.LOW)
                                       .provider(DigitalOutputProvider.class);

        this.red   = pi4j.create(redConfig);
        this.green = pi4j.create(greenConfig);
        this.blue  = pi4j.create(blueConfig);

        /* Simpliefied version
        this.red   = pi4j.dout().create(redPin);
        this.green = pi4j.dout().create(greenPin);
        this.blue  = pi4j.dout().create(bluePin);
        */
    }


    @Override public void setRed(final int red) {
        super.setRed(Helper.clamp(0, 1, red));
        if (red == 0) {
            this.red.low();
        } else {
            this.red.high();
        }
    }

    @Override public void setGreen(final int green) {
        super.setRed(Helper.clamp(0, 1, green));
        if (green == 0) {
            this.green.low();
        } else {
            this.green.high();
        }
    }

    @Override public void setBlue(final int blue) {
        super.setRed(Helper.clamp(0, 1, blue));
        if (blue == 0) {
            this.blue.low();
        } else {
            this.blue.high();
        }
    }

    @Override public void set(final int red, final int green, final int blue) {
        super.set(red, green, blue);
    }

    public void dispose() {
        pi4j.shutdown();
    }
}
