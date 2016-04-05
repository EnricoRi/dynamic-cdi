/*
 * Copyright [2014] [www.rapidpm.org / Sven Ruppert (sven.ruppert@rapidpm.org)]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package junit.org.rapidpm.ddi;

import org.junit.gen5.api.Assertions;
import org.junit.gen5.api.Test;
import org.rapidpm.ddi.DI;
import javax.inject.Inject;
import static org.junit.gen5.api.Assertions.assertEquals;
import static org.junit.gen5.api.Assertions.assertNotNull;

public class DITest002 extends DDIBaseTest {

  @Test
  public void testInjection001() throws Exception {
    Service service = new Service();
    DI.activateDI(service);

    Assertions.assertAll("SubServices",
        () -> assertNotNull(service.subService),
        () -> assertNotNull(service.subService.subSubService),
        () -> assertEquals("SubSubService test", service.work("test"))
    );
  }

  public static class Service {
    @Inject
    SubService subService;

    public String work(String txt) {
      return subService.work(txt);
    }
  }

  public static class SubService {
    @Inject
    SubSubService subSubService;

    public String work(final String txt) {
      return subSubService.work(txt);
    }
  }

  public static class SubSubService {
    public String work(final String txt) {
      return "SubSubService " + txt;
    }
  }
}
