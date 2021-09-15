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
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import eu.hansolo.iotmodules.sensors.Switch;


public class PushButton extends Switch {
    private Context      pi4j;
    private DigitalInput button;


    public PushButton(final String id, final Context pi4j, final int buttonPin) {
        super(id);
        this.pi4j = pi4j;
        DigitalInputConfigBuilder buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                                                             .id("button")
                                                             .name("Push button")
                                                             .address(buttonPin)
                                                             .pull(PullResistance.PULL_DOWN)
                                                             .debounce(3000L)
                                                             .provider("pigpio-digital-input");

        button = pi4j.create(buttonConfig);

        registerListeners();
    }

    private void registerListeners() {
        button.addListener(e -> setOn(DigitalState.LOW == e.state()));
    }
}
