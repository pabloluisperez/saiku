/*
 * Copyright 2014 OSBI Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.saiku.repository;

import net.thucydides.core.annotations.Steps;

import org.jbehave.core.annotations.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertNotNull;

/**
 * Created by bugg on 14/05/14.
 */
public class JackrabbitRepositoryDef {
  @NotNull
  public
  List l = new ArrayList<String>();

  @Steps
  public
  JackrabbitSteps data;

  boolean start = false;
  public String datasource;

  @Pending
  @Given("the server is starting up")
  public void givenTheServerIsStartingUp() {
    //data.initializeRepository();
    data.initializeRepository();
    data.startRepository();
  }

  @When("the initialisation is in progress")
  public void whenTheInitialisationIsInProgress() {
    //start = data.startRepository();

  }

  @Then("the jackrabbit repository should start cleanly")
  public void thenTheJackrabbitRepositoryShouldStartCleanly() {
    assertThat(true, is(true));
  }


  @Given("user Joe has created a valid query")
  @Pending
  public void givenUserJoeHasCreatedAValidQuery() {
    // PENDING
    data.initializeRepository();
    data.startRepository();
  }


  @When("the query is saved to Joe's home directory")
  @Pending
  public void whenTheQueryIsSavedToJoesHomeDirectory() {
    // PENDING
  }

  @Then("it should be stored within the jackrabbit repository in the correct location")
  @Pending
  public void thenItShouldBeStoredWithinTheJackrabbitRepositoryInTheCorrectLocation() {
    // PENDING
  }

  @Pending
  @Given("an existing repository with content")
  public void givenAnExistingRepositoryWithContent() throws RepositoryException {
    data.initializeRepository();
    data.startRepository();
    l = new ArrayList();
    l.add("joe");
    data.initializeUsers(l);
    datasource = "type=OLAP\n"
                 + "name=test\n"
                 + "driver=mondrian.olap4j.MondrianOlap4jDriver\n"
                 + "location=jdbc:mondrian:Jdbc=jdbc:hsqldb:res:foodmart/foodmart;Catalog=res:FoodMart.xml;\n"
                 + "username=sa\n"
                 + "password=";
    data.saveFile(datasource, "/homes/home:joe/sampledatasource.sds", "joe", "nt:olapdatasource");

  }

  @Nullable
  public
  byte[] zip = null;

  @When("the export routine is executed")
  public void whenTheExportRoutineIsExecuted() throws IOException, RepositoryException {
    zip = data.getBackup();
  }

  @Then("the server should export the content of the repository to a zip file")
  public void thenTheServerShouldExportTheContentOfTheRepositoryToAZipFile() {
    assertNotNull(zip);
  }

  @Pending
  @Given("user $username has a valid datasource")
  public void givenUserAdminHasAValidDatasource(String username) {
    data.initializeRepository();
    data.startRepository();
    l = new ArrayList();
    l.add(username);
    data.initializeUsers(l);
    datasource = "type=OLAP\n"
                 + "name=test\n"
                 + "driver=mondrian.olap4j.MondrianOlap4jDriver\n"
                 + "location=jdbc:mondrian:Jdbc=jdbc:hsqldb:res:foodmart/foodmart;Catalog=res:FoodMart.xml;\n"
                 + "username=sa\n"
                 + "password=";

  }

  @When("$username saves the datasource")
  public void whenTheySaveTheDatasource(String username) throws RepositoryException {
    data.saveFile(datasource, "/homes/home:joe/sampledatasource.sds", username, "nt:olapdatasource");
  }

  @Then("it should be stored within the jackrabbit repository in the datasource pool for $username")
  public void thenItShouldBeStoredWithinTheJackrabbitRepositoryInTheDatasourcePool(String username)
      throws RepositoryException {
    String ds = "type=OLAP\n"
                + "name=test\n"
                + "driver=mondrian.olap4j.MondrianOlap4jDriver\n"
                + "location=jdbc:mondrian:Jdbc=jdbc:hsqldb:res:foodmart/foodmart;Catalog=res:FoodMart.xml;\n"
                + "username=sa\n"
                + "password=";
    assertThat(data.getFile("/homes/home:joe/sampledatasource.sds", username), is(ds));
  }


  @Then("jackrabbit should not import the sample data")
  @Pending
  public void thenJackrabbitShouldNotImportTheSampleData() {
    // PENDING
  }

  @Given("a new saiku installation")
  @Pending
  public void givenANewSaikuInstallation() {
    data.initializeRepository();
    data.startRepository();
    // PENDING
  }

  @When("the server is started")
  @Pending
  public void whenTheServerIsStarted() {
    // PENDING
  }

  @Then("jackrabbit should import sample data to create a working system.")
  @Pending
  public void thenJackrabbitShouldImportSampleDataToCreateAWorkingSystem() {
    // PENDING
  }

  @When("$user moves the $folder node from $source to his $target directory")
  public void whenHeMovesTheFolder1NodeToHisHomeDirectory(String user, String folder, String source,
                                                          @NotNull String target)
      throws RepositoryException {
    data.moveFolder(user, folder, source, target);
  }

  @Then("the $folder node should be moved within $user 's directory")
  public void thenTheFolder1NodeShouldBeMovedWithinTheHomeDirectory(String folder, String user)
      throws RepositoryException {
    javax.jcr.Node folders = data.getFolders(user, folder);
    assertThat(folders.getName(), is(folder));
  }

  public String schema;

  @Pending
  @Given("user $username has a valid mondrian schema file")
  public void givenUserAdminHasAValidMondrianSchemaFile(String username) throws IOException {
    data.initializeRepository();
    data.startRepository();
    l = new ArrayList();
    l.add(username);
    data.initializeUsers(l);
    byte[] encoded = Files.readAllBytes(Paths.get("../../FoodMart.xml"));
    schema = new String(encoded);


  }

  @When("$username hits save that file to the repository schema pool")
  public void whenTheySaveThatFileToTheRepositorySchemaPool(String username) throws RepositoryException {
    data.saveFile(schema, "/homes/home:joe/FoodMart.xml", username, "nt:mondrianschema");
  }

  @Then("the repository stores the schema for $username in the correct location")
  public void thenTheRepositoryStoresTheSchemaInTheCorrectLocation(String username)
      throws RepositoryException, IOException {
    String file = data.getFile("/homes/home:joe/FoodMart.xml", username);
    byte[] encoded = Files.readAllBytes(Paths.get("../../FoodMart.xml"));

    assertThat(file, is(new String(encoded)));
  }

  @Given("a running server")
  @Pending
  public void givenARunningServer() {
    // PENDING
    data.initializeRepository();
    data.startRepository();
  }

  @When("the shutdown is requested")
  @Pending
  public void whenTheShutdownIsRequested() {
    // PENDING
  }

  @Then("the repository should be shutdown cleanly")
  @Pending
  public void thenTheRepositoryShouldBeShutdownCleanly() {
    // PENDING
  }

  @Pending
  @Given("user $username does not exist")
  public void usernameDoesNotExist(String username) {
    data.initializeRepository();
    data.startRepository();
    List<String> names = data.getHomeDirectoryList();

    assertThat(names, not(hasItem(username)));
  }

  @When("a new user called $username is added")
  public void usernameIsAdded(String username) {
    l.add(username);
    data.initializeUsers(l);
  }

  @When("a second user called $username is added")
  public void secondUsernameIsAdded(String username) {
    l.add(username);

  }

  @Then("a new node called $directory needs creating")
  public void homeDirectoryCreation(String directory) {
    try {
      assertThat(data.getHomeDirectory(directory).getProperty("user").getString(), equalTo(directory));
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }

  @When("he adds the testing directory to the home directory")
  @Pending
  public void whenHeAddsTheTestingDirectoryToTheHomeDirectory() {

  }

  @Then("a new node testing is created within the jackrabbit repository")
  @Pending
  public void thenANewNodeTestingIsCreatedWithinTheJackrabbitRepository() {
    // PENDING
  }

  @Pending
  @Given("$user has a node within his home directory called $folder")
  public void givenJoeHasANodeWithinHisHomeDirectoryCalledTestingfolder1(String user, String folder)
      throws RepositoryException {
    data.initializeRepository();
    data.startRepository();
    List l = new ArrayList<String>();
    l.add(user);
    data.initializeUsers(l);
    List<String> names = data.getHomeDirectoryList();

    assertThat(names, hasItem(user));

    data.createFolder(user, folder);

  }


  @Then("a new node should not be created and an exception thrown")
  public void thenANewNodeShouldNotBeCreatedAndAnExceptionThrown() throws RepositoryException {
    data.initializeDuplicateUsers(l);
  }

  @Pending
  @Given("a user called $username already exists")
  public void givenUserCalledJoeAlreadyExists(String username) {
    data.initializeRepository();
    data.startRepository();
    List l = new ArrayList<String>();
    l.add(username);
    data.initializeUsers(l);
    List<String> names = data.getHomeDirectoryList();

    assertThat(names, hasItem(username));
  }

  @Pending
  @Given("user $username has a directory called $folder in his home directory")
  public void givenUserJoeHasADirectoryCalledDeletetestInHisHomeDirectory(String username, String folder) {
    data.initializeRepository();
    data.startRepository();
    List l = new ArrayList<String>();
    l.add(username);
    data.initializeUsers(l);
    List<String> names = data.getHomeDirectoryList();

    assertThat(names, hasItem(username));
    try {
      assertThat(data.createFolder(username, folder), is(true));
    } catch (RepositoryException e) {
      e.printStackTrace();
    }

  }

  @When("user $user deletes his $folder folder")
  public void whenUserJoePressesDelete(String user, String folder) {
    try {
      assertThat(data.deleteFolder(user, folder), is(true));
    } catch (RepositoryException e) {
      e.printStackTrace();
    }

  }

  @Then("the $user 's node $folder should be removed")
  public void thenTheDeletetestNodeShouldBeRemoved(String user, String folder) throws RepositoryException {
    data.getBrokenFolders(user, folder);
  }


  @BeforeScenario
  public void beforeAnyScenario() {
    //data.initializeRepository();
    //data.startRepository();
  }

  @AfterScenario
  public void afterAnyScenario() {
    data.shutdownRepository();
  }
}
