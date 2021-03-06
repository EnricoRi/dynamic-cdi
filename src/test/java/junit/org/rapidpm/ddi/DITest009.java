/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package junit.org.rapidpm.ddi;

import org.junit.Test;
import org.rapidpm.ddi.DDIModelException;
import org.rapidpm.ddi.DI;

import javax.inject.Inject;

public class DITest009 {

  @Test(expected = DDIModelException.class)
  public void test001() throws Exception {
    Service service = new Service();
    DI.activateDI(service);

  }


  public interface SubService {
  }

  public class Service {
    @Inject SubService suService;
  }

  public class FaultyServiceImpl implements SubService{
    public FaultyServiceImpl() {
      throw new RuntimeException("OOOPS");
    }
  }
}
