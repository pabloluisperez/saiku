/*
 *   Copyright 2014 OSBI Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.saiku.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

/**
 * AnonymousSessionService.
 */
public class AnonymousSessionService implements ISessionService {


  @NotNull
  private final
  HashMap<String, Object> session = new HashMap<String, Object>();

  public AnonymousSessionService() {
    session.put("username", "anonymous");
    session.put("sessionid", UUID.randomUUID().toString());
    session.put("roles", new ArrayList<String>());


  }

  @Nullable
  public Map<String, Object> login(HttpServletRequest req, String username,
                                   String password) {
    // TODO Auto-generated method stub
    return null;
  }


public String generateToken(HttpServletRequest req, String username, String password) throws Exception {
	// TODO Auto-generated method stub
	return null;
}

public Map<String, Object> loginToken(HttpServletRequest req, String token) throws Exception {
	// TODO Auto-generated method stub
	return null;
}  
  
  public void logout( HttpServletRequest req ) {
    // TODO Auto-generated method stub

  }

  public void authenticate(HttpServletRequest req, String username,
                           String password) {
    // TODO Auto-generated method stub

  }

  @NotNull
  public Map<String, Object> getSession() {
    return session;
  }

  @NotNull
  public Map<String, Object> getAllSessionObjects() {
    return session;
  }

}
