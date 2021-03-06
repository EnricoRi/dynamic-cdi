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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rapidpm.ddi.DI;

import javax.inject.Inject;

public class DITest005 {

  @Inject Service service;

  @Before
  public void setUp() throws Exception {
    DI.clearReflectionModel();
    DI.activateDI(this);
  }

  @Test
  public void test001() throws Exception {
    Assert.assertNotNull(service);
    Assert.assertEquals(service.getClass(), ServiceImpl_A.class);
  }

  public interface Service {
    String doWork(String txt);
  }

  public static class ServiceImpl_A implements Service {
    @Override
    public String doWork(final String txt) {
      return "Nase - " + txt;
    }
  }

}
