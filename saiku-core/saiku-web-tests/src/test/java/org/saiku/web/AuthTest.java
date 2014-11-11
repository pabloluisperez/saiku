/*  
 *   Copyright 2012 OSBI Ltd
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
package org.saiku.web;

import org.junit.Test;

public class AuthTest extends AbstractServiceTest {

  @Test
  public void testHappyDay() throws Exception {
        /*int port = 9999;
        Client client = Client.create();
        client.setFollowRedirects(false);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("grant_type", "password");
        formData.add("client_id", "my-trusted-client");
        formData.add("username", "marissa");
        formData.add("password", "koala");
        WebResource webResource = client.resource("http://localhost:9999/");
        ClientResponse response = webResource.path("/saiku/oauth/authorize")
          .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
          .post(ClientResponse.class, formData);
        assertEquals(200, response.getClientResponseStatus().getStatusCode());
        assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));

        DefaultOAuth2SerializationService serializationService = new DefaultOAuth2SerializationService();
        OAuth2AccessToken accessToken = serializationService.deserializeJsonAccessToken(response.getEntityInputStream());

        //now try and use the token to access a protected resource.

        //first make sure the resource is actually protected.
        response = client.resource("http://localhost:" + port + "/saiku/serverdocs/index.html").get(ClientResponse.class);
        assertFalse(200 == response.getClientResponseStatus().getStatusCode());

        //now make sure an authorized request is valid.
        response = client.resource("http://localhost:" + port + "/saiku/serverdocs/index.html")
          .header("Authorization", String.format("OAuth %s", accessToken.getValue()))
          .get(ClientResponse.class);
        assertEquals(200, response.getClientResponseStatus().getStatusCode());
        */
  }


  /**
   * tests that an error occurs if you attempt to use username/password creds for a non-password grant type.
   */
  @Test
  public void testInvalidGrantType() throws Exception {
      /* int port = 9999;
      Client client = Client.create();
      client.setFollowRedirects(false);

      MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
      formData.add("grant_type", "authorization_code");
      formData.add("client_id", "my-trusted-client");
      formData.add("username", "marissa");
      formData.add("password", "koala");
      ClientResponse response = client.resource("http://localhost:" + port + "/saiku/oauth/authorize")
        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        .post(ClientResponse.class, formData);
      assertEquals(400, response.getClientResponseStatus().getStatusCode());
      List<NewCookie> newCookies = response.getCookies();
      if (!newCookies.isEmpty()) {
        fail("No cookies should be set. Found: " + newCookies.get(0).getName() + ".");
      }
      assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));

      DefaultOAuth2SerializationService serializationService = new DefaultOAuth2SerializationService();
      try {
        throw serializationService.deserializeJsonError(response.getEntityInputStream());
      }
      catch (OAuth2Exception e) {
        assertEquals("invalid_request", e.getOAuth2ErrorCode());
      }
      */
  }

}
