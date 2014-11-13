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


import org.saiku.database.dto.MondrianSchema;
import org.saiku.datasources.connection.RepositoryFile;
import org.saiku.service.user.UserService;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jcr.*;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * JackRabbit JCR Repository Manager for Saiku.
 */
public class JackRabbitRepositoryManager implements IRepositoryManager {

  private static final Logger LOG = LoggerFactory.getLogger(JackRabbitRepositoryManager.class);
  private static JackRabbitRepositoryManager ref;
  @Nullable
  private Repository repository;
  @Nullable
  private Session session;
  private Node root;
  private UserService userService;

  private JackRabbitRepositoryManager() {

  }

  /**
   * TODO this is currently threadsafe but to improve performance we should split it up to allow multiple sessions to
   * hit the repo at the same time.
   */
  public static synchronized JackRabbitRepositoryManager getJackRabbitRepositoryManager() {
    if (ref == null) {
      ref = new JackRabbitRepositoryManager();
    }
    return ref;
  }

  @NotNull
  public Object clone()
      throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
    // that'll teach 'em
  }

  public void init() {

  }

  void login() throws RepositoryException {
    session = repository.login(
        new SimpleCredentials("admin", "admin".toCharArray()));
  }


  public boolean start(UserService userService) throws RepositoryException {
    this.userService = userService;
    if (session == null) {
      LOG.info("starting repo");
      String xml = "../../repository/configuration.xml";
      String dir = "../../repository/data";
      RepositoryConfig config = RepositoryConfig.create(xml, dir);
      repository = RepositoryImpl.create(config);
      LOG.info("repo started");
      LOG.info("logging in");
      login();
      LOG.info("logged in");
      root = session.getRootNode();

      root.getSession().save();
      createFiles();
      createFolders();
      createNamespace();
      createSchemas();
      createDataSources();

      Node n = JcrUtils.getOrAddFolder(root, "homes");
      n.addMixin("nt:saikufolders");

      Map<String, List<AclMethod>> m = new HashMap();
      ArrayList l = new ArrayList();
      l.add(AclMethod.READ);
      m.put("ROLE_USER", l);
      AclEntry e = new AclEntry("admin", AclType.SECURED, m, null);

      Acl2 acl2 = new Acl2(n);
      acl2.addEntry(n.getPath(), e);
      acl2.serialize(n);

      n = JcrUtils.getOrAddFolder(root, "datasources");
      n.addMixin("nt:saikufolders");

      n = JcrUtils.getOrAddFolder(root, "etc");
      n.addMixin("nt:saikufolders");
      n = JcrUtils.getOrAddFolder(n, "legacyreports");
      n.addMixin("nt:saikufolders");

      session.save();
      LOG.info("node added");
    }
    return true;

  }

  public void createUser(String u) throws RepositoryException {
    login();
    Node parent = JcrUtils.getNodeIfExists(root, "homes");
    if (parent != null) {
      Node node = parent.addNode("home:" + u, "nt:folder");
      node.addMixin("nt:saikufolders");
      //node.setProperty("type", "homedirectory");
      //node.setProperty("user", u);
      AclEntry e = new AclEntry(u, AclType.PRIVATE, null, null);

      Acl2 acl2 = new Acl2(node);
      acl2.addEntry(node.getPath(), e);
      acl2.serialize(node);

      node.getSession().save();

    } else {
      //TODO CANT CREATE DIRECTORY
    }
  }

  public javax.jcr.NodeIterator getHomeFolders() throws RepositoryException {
    //login();
    Node homes = root.getNode("homes");
    return homes.getNodes();
  }

  public Node getHomeFolder(String path) throws RepositoryException {
    return root.getNode("homes").getNode("home:" + path);
  }

  public Node getFolder(String user, String directory) throws RepositoryException {
    return getHomeFolder(user).getNode(directory);
  }

  Node getFolderNode(@NotNull String directory) throws RepositoryException {
    if (directory.startsWith("/")) {
      directory = directory.substring(1, directory.length());
    }
    return root.getNode(directory);
  }

  public void shutdown() {
    ((JackrabbitRepository) repository).shutdown();
      /*  String repositoryLocation = ((TransientRepository) repository).getHomeDir();
        try {
            FileUtils.deleteDirectory(new File(repositoryLocation));
        } catch (final IOException e) {
            System.out.println(e.getLocalizedMessage());
            //TODO FIX
        }*/
    repository = null;
    session = null;
  }

  public boolean createFolder(String username, @NotNull String folder) throws RepositoryException {
    Node userfolder = getHomeFolder(username);
    String[] path = folder.split("/");
    Node nest = null;
    for (String p : path) {
      if (nest == null) {
        nest = userfolder.addNode(p);
      } else {
        nest.addNode(p);
      }

    }

    userfolder.getSession().save();
    return true;
  }

  public boolean deleteFolder(@NotNull String folder) throws RepositoryException {
    if (folder.startsWith("/")) {
      folder = folder.substring(1, folder.length());
    }
        /*Node n;
        try {

            n = getFolder(folder);
            n.remove();
        } catch (RepositoryException e) {
            LOG.error("Could not remove folder: "+folder, e);
        }*/
    Node node = JcrUtils.getNodeIfExists(root, folder);
    if (node != null) {
      node.remove();
      node.getSession().save();
      return true;
    } else {
      return false;
    }
  }

  public void deleteRepository() throws RepositoryException {
    while (root.getNodes().hasNext()) {
      root.getNodes().nextNode().remove();
    }
  }

  public boolean moveFolder(String user, String folder, String source, @Nullable String target)
      throws RepositoryException {
    Node root = getHomeFolder(user).getNode(source + "/" + folder);

    if (target == null) {
      //session.getWorkspace().move(root.getPath(), root.getSession().getRootNode().getPath()+
      // "/homes/home:"+user+"/"+folder);
      root.getSession().move(root.getPath(), getHomeFolder(user).getPath() + "/" + root.getName());
      root.getSession().save();
    } else {
      root.getSession().move(root.getPath(), getHomeFolder(user).getPath());
      root.getSession().save();
    }

    return true;
  }

  public Node saveFile(@Nullable Object file, @NotNull String path, String user, @NotNull String type,
                       @NotNull List<String> roles)
      throws RepositoryException {
    if (file == null) {
      //Create new folder
      String parent = path.substring(0, path.lastIndexOf("/"));
      Node node = getFolder(parent);
      Acl2 acl2 = new Acl2(node);
      acl2.setAdminRoles(userService.getAdminRoles());
      if (acl2.canWrite(node, user, roles)) {
        //TODO Throw exception
      }

      int pos = path.lastIndexOf("/");
      String filename = "./" + path.substring(pos + 1, path.length());
      Node resNode = node.addNode(filename, "nt:folder");
      resNode.addMixin("nt:saikufolders");
      return resNode;

    } else {
      int pos = path.lastIndexOf("/");
      String filename = "./" + path.substring(pos + 1, path.length());
      Node n = getFolder(path.substring(0, pos));
      Acl2 acl2 = new Acl2(n);
      acl2.setAdminRoles(userService.getAdminRoles());
      if (acl2.canWrite(n, user, roles)) {
        //TODO Throw exception
      }


      Node resNode = n.addNode(filename, "nt:file");
      if (type.equals("nt:saikufiles")) {
        resNode.addMixin("nt:saikufiles");
      } else if (type.equals("nt:mondrianschema")) {
        resNode.addMixin("nt:mondrianschema");
      } else if (type.equals("nt:olapdatasource")) {
        resNode.addMixin("nt:olapdatasource");
      }
      Node contentNode = resNode.addNode("jcr:content", "nt:resource");

      //resNode.setProperty ("jcr:mimeType", "text/plain");
      //resNode.setProperty ("jcr:encoding", "utf8");
      contentNode.setProperty("jcr:data", (String) file);
        /*Calendar lastModified = Calendar.getInstance ();
        lastModified.setTimeInMillis (new Date().getTime());
        resNode.setProperty ("jcr:lastModified", lastModified);*/
      resNode.getSession().save();
      return resNode;
    }
  }

  public void removeFile(String path, String user, @NotNull List<String> roles) throws RepositoryException {
    Node node = getFolder(path);
    Acl2 acl2 = new Acl2(node);
    acl2.setAdminRoles(userService.getAdminRoles());
    if (!acl2.canRead(node, user, roles)) {
      //TODO Throw exception
      throw new RepositoryException();

    }

    node.remove();

    node.getSession().save();


  }

  public void moveFile(String source, String target, String user, @NotNull List<String> roles)
      throws RepositoryException {
    Node node = getFolder(source);
    Node t = getFolder(target);
    Acl2 acl2 = new Acl2(node);
    acl2.setAdminRoles(userService.getAdminRoles());
    if (!acl2.canRead(node, user, roles) || acl2.canWrite(t, user, roles)) {
      //TODO Throw exception
      throw new RepositoryException();

    }

    node.getSession().move(source, target + "/" + node.getName());

    node.getSession().save();


  }


  public Node saveInternalFile(@Nullable Object file, @NotNull String path, @Nullable String type)
      throws RepositoryException {
    if (file == null) {
      //Create new folder
      String parent = path.substring(0, path.lastIndexOf("/"));
      Node node = getFolder(parent);

      int pos = path.lastIndexOf("/");
      String filename = "./" + path.substring(pos + 1, path.length());
      Node resNode = node.addNode(filename, "nt:folder");
      resNode.addMixin("nt:saikufolders");
      return resNode;

    } else {
      int pos = path.lastIndexOf("/");
      String filename = "./" + path.substring(pos + 1, path.length());
      Node n = getFolder(path.substring(0, pos));


      if (type == null) {
        type = "";
      }
      Node resNode = n.addNode(filename, "nt:file");
      if (type.equals("nt:saikufiles")) {
        resNode.addMixin("nt:saikufiles");
      } else if (type.equals("nt:mondrianschema")) {
        resNode.addMixin("nt:mondrianschema");
      } else if (type.equals("nt:olapdatasource")) {
        resNode.addMixin("nt:olapdatasource");
      } else if (!type.equals("")) {
        resNode.addMixin(type);
      }
      Node contentNode = resNode.addNode("jcr:content", "nt:resource");

      //resNode.setProperty ("jcr:mimeType", "text/plain");
      //resNode.setProperty ("jcr:encoding", "utf8");
      contentNode.setProperty("jcr:data", (String) file);
        /*Calendar lastModified = Calendar.getInstance ();
        lastModified.setTimeInMillis (new Date().getTime());
        resNode.setProperty ("jcr:lastModified", lastModified);*/
      resNode.getSession().save();
      return resNode;
    }
  }

  public String getFile(String s, String username, @NotNull List<String> roles) throws RepositoryException {
    Node node = getFolder(s);
    Acl2 acl2 = new Acl2(node);
    acl2.setAdminRoles(userService.getAdminRoles());
    if (!acl2.canRead(node, username, roles)) {
      //TODO Throw exception
      throw new RepositoryException();
    }
    return getFolder(s).getNodes("jcr:content").nextNode().getProperty("jcr:data").getString();
  }

  public String getInternalFile(String s) throws RepositoryException {

    return getFolder(s).getNodes("jcr:content").nextNode().getProperty("jcr:data").getString();
  }

  public void removeInternalFile(String s) throws RepositoryException {
    Node n = getFolder(s);
    n.remove();
    n.getSession().save();
  }

  @NotNull
  public List<MondrianSchema> getAllSchema() throws RepositoryException {
    QueryManager qm = session.getWorkspace().getQueryManager();
    String sql = "SELECT * FROM [nt:mondrianschema]";
    Query query = qm.createQuery(sql, Query.JCR_SQL2);

    QueryResult res = query.execute();

    NodeIterator node = res.getNodes();

    List<MondrianSchema> l = new ArrayList<MondrianSchema>();
    while (node.hasNext()) {
      Node n = node.nextNode();
      String p = n.getPath();

      MondrianSchema m = new MondrianSchema();
      m.setName(n.getName());
      m.setPath(p);

      l.add(m);

    }
    return l;
  }

  @NotNull
  public List<IRepositoryObject> getAllFiles(@NotNull String type, String username, @NotNull List<String> roles) {
    return getRepoObjects(root, type, username, roles);
  }

  public void deleteFile(String datasourcePath) {
    Node n;
    try {
      n = getFolder(datasourcePath);
      n.remove();
    } catch (RepositoryException e) {
      LOG.error("Could not remove file " + datasourcePath, e);
    }

  }

  @Nullable
  private AclEntry getAclObj(@NotNull String path) {
    Node node = null;
    try {
      node = getFolderNode(path);
    } catch (RepositoryException e) {
      LOG.error("Could not get file", e);
    }
    Acl2 acl2 = new Acl2(node);
    acl2.setAdminRoles(userService.getAdminRoles());
    AclEntry entry = acl2.getEntry(path);
    if (entry == null) {
      entry = new AclEntry();
    }
    return entry;
  }

  @Nullable
  public AclEntry getACL(@NotNull String object, String username, @NotNull List<String> roles) {


    Node node = null;
    try {
      node = getFolderNode(object);
    } catch (RepositoryException e) {
      LOG.error("Could not get file/folder", e);
    }
    Acl2 acl2 = new Acl2(node);
    acl2.setAdminRoles(userService.getAdminRoles());

    if (acl2.canGrant(node, username, roles)) {
      return getAclObj(object);
    }

    return null;
  }

  public void setACL(@NotNull String object, String acl, String username, @NotNull List<String> roles)
      throws RepositoryException {


    ObjectMapper mapper = new ObjectMapper();
    LOG.debug("Set ACL to " + object + " : " + acl);
    AclEntry ae = null;
    try {
      ae = mapper.readValue(acl, AclEntry.class);
    } catch (IOException e) {
      LOG.error("Could not read ACL blob", e);
    }

    Node node = null;
    try {
      node = getFolderNode(object);
    } catch (RepositoryException e) {
      LOG.error("Could not get file/folder " + object, e);
    }

    Acl2 acl2 = new Acl2(node);
    acl2.setAdminRoles(userService.getAdminRoles());

    if (acl2.canGrant(node, username, roles)) {
      if (node != null) {
        acl2.addEntry(object, ae);
        acl2.serialize(node);
      }
    }

    if (node != null) {
      node.getSession().save();
    }
  }

  @NotNull
  public List<MondrianSchema> getInternalFilesOfFileType(String type) throws RepositoryException {
    QueryManager qm = session.getWorkspace().getQueryManager();
    String sql = "SELECT * FROM [nt:mongoschema]";
    Query query = qm.createQuery(sql, Query.JCR_SQL2);

    QueryResult res = query.execute();

    NodeIterator node = res.getNodes();

    List<MondrianSchema> l = new ArrayList<MondrianSchema>();
    while (node.hasNext()) {
      Node n = node.nextNode();
      String p = n.getPath();

      MondrianSchema m = new MondrianSchema();
      m.setName(n.getName());
      m.setPath(p);
      m.setType(type);
      l.add(m);

    }
    return l;
  }


  @NotNull
  public List<DataSource> getAllDataSources() throws RepositoryException {
    QueryManager qm = session.getWorkspace().getQueryManager();
    String sql = "SELECT * FROM [nt:olapdatasource]";
    Query query = qm.createQuery(sql, Query.JCR_SQL2);

    QueryResult res = query.execute();

    NodeIterator node = res.getNodes();

    List<DataSource> ds = new ArrayList<DataSource>();
    while (node.hasNext()) {
      Node n = node.nextNode();
      JAXBContext jaxbContext = null;
      Unmarshaller jaxbMarshaller = null;
      try {
        jaxbContext = JAXBContext.newInstance(DataSource.class);
      } catch (JAXBException e) {
        LOG.error("Could not read XML", e);
      }
      try {
        jaxbMarshaller = jaxbContext != null ? jaxbContext.createUnmarshaller() : null;
      } catch (JAXBException e) {
        LOG.error("Could not read XML", e);
      }
      InputStream stream =
          new ByteArrayInputStream(
              n.getNodes("jcr:content").nextNode().getProperty("jcr:data").getString().getBytes());
      DataSource d = null;
      try {
        d = (DataSource) (jaxbMarshaller != null ? jaxbMarshaller.unmarshal(stream) : null);
      } catch (JAXBException e) {
        LOG.error("Could not read XML", e);
      }

      if (d != null) {
        d.setPath(n.getPath());
      }
      ds.add(d);

    }

    return ds;
  }

  public void saveDataSource(DataSource ds, @NotNull String path) throws RepositoryException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(DataSource.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

      // output pretty printed
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

      jaxbMarshaller.marshal(ds, baos);


    } catch (JAXBException e) {
      LOG.error("Could not read XML", e);
    }

    int pos = path.lastIndexOf("/");
    String filename = "./" + path.substring(pos + 1, path.length());
    Node n = getFolder(path.substring(0, pos));
    Node resNode = n.addNode(filename, "nt:file");

    resNode.addMixin("nt:olapdatasource");

    Node contentNode = resNode.addNode("jcr:content", "nt:resource");

    //resNode.setProperty ("jcr:mimeType", "text/plain");
    //resNode.setProperty ("jcr:encoding", "utf8");
    contentNode.setProperty("jcr:data", baos.toString());
        /*Calendar lastModified = Calendar.getInstance ();
        lastModified.setTimeInMillis (new Date().getTime());
        resNode.setProperty ("jcr:lastModified", lastModified);*/
    resNode.getSession().save();

  }

  public byte[] exportRepository() throws RepositoryException, IOException {
    final ByteArrayOutputStream os2 = new ByteArrayOutputStream();
    final OutputStream os = new ByteArrayOutputStream();
    session.exportDocumentView("/", os, false, false);
    ZipOutputStream zs = new ZipOutputStream(os2);
    ZipEntry e = new ZipEntry("backup.xml");
    zs.putNextEntry(e);
    zs.write(os.toString().getBytes());
    zs.closeEntry();
    zs.close();
    return os2.toByteArray();
  }

  public void restoreRepository(byte[] xml) throws RepositoryException, IOException {
    InputStream stream = new ByteArrayInputStream(xml);
    session.importXML("/", stream, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
  }

  @Nullable
  public RepositoryFile getFile(String fileUrl) {
    Node n = null;
    try {
      n = getFolder(fileUrl);
    } catch (RepositoryException e) {
      e.printStackTrace();
    }

    try {
      return new RepositoryFile(n != null ? n.getName() : null, null, fileUrl);
    } catch (RepositoryException e) {
      e.printStackTrace();
    }

    return null;
  }

  Node getFolder(String path) throws RepositoryException {
    return session.getNode(path);
  }

  @Nullable
  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  void createNamespace() throws RepositoryException {
    NamespaceRegistry ns = session.getWorkspace().getNamespaceRegistry();

    if (!Arrays.asList(ns.getPrefixes()).contains("home")) {
      ns.registerNamespace("home", "http://www.meteorite.bi/namespaces/home");
    }
  }

  void createDataSources() throws RepositoryException {

    NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
    NodeTypeTemplate ntt = manager.createNodeTypeTemplate();
    ntt.setName("nt:olapdatasource");

    String[] str = new String[] { "nt:file" };
    ntt.setDeclaredSuperTypeNames(str);
    ntt.setMixin(true);

    PropertyDefinitionTemplate pdt3 = manager.createPropertyDefinitionTemplate();

    pdt3.setName("jcr:data");
    pdt3.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt4 = manager.createPropertyDefinitionTemplate();

    pdt4.setName("enabled");
    pdt4.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt5 = manager.createPropertyDefinitionTemplate();

    pdt5.setName("owner");
    pdt5.setRequiredType(PropertyType.STRING);


    ntt.getPropertyDefinitionTemplates().add(pdt3);
    ntt.getPropertyDefinitionTemplates().add(pdt4);
    ntt.getPropertyDefinitionTemplates().add(pdt5);
    try {
      manager.registerNodeType(ntt, false);
    } catch (NodeTypeExistsException e) {
      LOG.error("Node Exists", e);
    }
  }

  void createSchemas() throws RepositoryException {

    NodeTypeManager manager =
        session.getWorkspace().getNodeTypeManager();
    NodeTypeTemplate ntt = manager.createNodeTypeTemplate();
    ntt.setName("nt:mondrianschema");
    //ntt.setPrimaryItemName("nt:file");
    String[] str = new String[] { "nt:file" };
    ntt.setDeclaredSuperTypeNames(str);
    ntt.setMixin(true);
    PropertyDefinitionTemplate pdt = manager.createPropertyDefinitionTemplate();

    pdt.setName("schemaname");
    pdt.setRequiredType(PropertyType.STRING);
    pdt.isMultiple();
    PropertyDefinitionTemplate pdt2 = manager.createPropertyDefinitionTemplate();

    pdt2.setName("cubenames");
    pdt2.setRequiredType(PropertyType.STRING);
    pdt2.isMultiple();

    PropertyDefinitionTemplate pdt3 = manager.createPropertyDefinitionTemplate();

    pdt3.setName("jcr:data");
    pdt3.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt4 = manager.createPropertyDefinitionTemplate();
    pdt4.setName("owner");
    pdt4.setRequiredType(PropertyType.STRING);

    ntt.getPropertyDefinitionTemplates().add(pdt);
    ntt.getPropertyDefinitionTemplates().add(pdt2);
    ntt.getPropertyDefinitionTemplates().add(pdt3);
    ntt.getPropertyDefinitionTemplates().add(pdt4);


    try {
      manager.registerNodeType(ntt, false);
    } catch (NodeTypeExistsException e) {
      LOG.error("Node Exists", e);
    }
  }

  void createFiles() throws RepositoryException {

    NodeTypeManager manager =
        session.getWorkspace().getNodeTypeManager();
    NodeTypeTemplate ntt = manager.createNodeTypeTemplate();
    ntt.setName("nt:saikufiles");
    String[] str = new String[] { "nt:file" };
    ntt.setDeclaredSuperTypeNames(str);
    ntt.setMixin(true);
    PropertyDefinitionTemplate pdt = manager.createPropertyDefinitionTemplate();
    pdt.setName("owner");
    pdt.setRequiredType(PropertyType.STRING);


    PropertyDefinitionTemplate pdt2 = manager.createPropertyDefinitionTemplate();
    pdt2.setName("type");
    pdt2.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt4 = manager.createPropertyDefinitionTemplate();
    pdt4.setName("roles");
    pdt4.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt5 = manager.createPropertyDefinitionTemplate();
    pdt5.setName("users");
    pdt5.setRequiredType(PropertyType.STRING);


    PropertyDefinitionTemplate pdt3 = manager.createPropertyDefinitionTemplate();
    pdt3.setName("jcr:data");
    pdt3.setRequiredType(PropertyType.STRING);

    ntt.getPropertyDefinitionTemplates().add(pdt);
    ntt.getPropertyDefinitionTemplates().add(pdt2);
    ntt.getPropertyDefinitionTemplates().add(pdt3);
    ntt.getPropertyDefinitionTemplates().add(pdt4);
    ntt.getPropertyDefinitionTemplates().add(pdt5);

    try {
      manager.registerNodeType(ntt, false);
    } catch (NodeTypeExistsException e) {
      LOG.error("Node Exists", e);
    }
  }

  public void createFileMixin(String type) throws RepositoryException {

    NodeTypeManager manager =
        session.getWorkspace().getNodeTypeManager();
    NodeTypeTemplate ntt = manager.createNodeTypeTemplate();
    ntt.setName(type);
    String[] str = new String[] { "nt:file" };
    ntt.setDeclaredSuperTypeNames(str);
    ntt.setMixin(true);
    PropertyDefinitionTemplate pdt = manager.createPropertyDefinitionTemplate();
    pdt.setName("owner");
    pdt.setRequiredType(PropertyType.STRING);


    PropertyDefinitionTemplate pdt2 = manager.createPropertyDefinitionTemplate();
    pdt2.setName("type");
    pdt2.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt4 = manager.createPropertyDefinitionTemplate();
    pdt4.setName("roles");
    pdt4.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt5 = manager.createPropertyDefinitionTemplate();
    pdt5.setName("users");
    pdt5.setRequiredType(PropertyType.STRING);


    PropertyDefinitionTemplate pdt3 = manager.createPropertyDefinitionTemplate();
    pdt3.setName("jcr:data");
    pdt3.setRequiredType(PropertyType.STRING);

    ntt.getPropertyDefinitionTemplates().add(pdt);
    ntt.getPropertyDefinitionTemplates().add(pdt2);
    ntt.getPropertyDefinitionTemplates().add(pdt3);
    ntt.getPropertyDefinitionTemplates().add(pdt4);
    ntt.getPropertyDefinitionTemplates().add(pdt5);

    try {
      manager.registerNodeType(ntt, false);
    } catch (NodeTypeExistsException e) {
      LOG.error("Node Exists", e);
    }
  }

  @Nullable
  public Object getRepositoryObject() {
    return repository;
  }

  void createFolders() throws RepositoryException {

    NodeTypeManager manager =
        session.getWorkspace().getNodeTypeManager();
    NodeTypeTemplate ntt = manager.createNodeTypeTemplate();
    ntt.setName("nt:saikufolders");
    String[] str = new String[] { "nt:folder" };
    ntt.setDeclaredSuperTypeNames(str);
    ntt.setMixin(true);
    PropertyDefinitionTemplate pdt = manager.createPropertyDefinitionTemplate();
    pdt.setName("owner");
    pdt.setRequiredType(PropertyType.STRING);


    PropertyDefinitionTemplate pdt2 = manager.createPropertyDefinitionTemplate();
    pdt2.setName("type");
    pdt2.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt4 = manager.createPropertyDefinitionTemplate();
    pdt4.setName("roles");
    pdt4.setRequiredType(PropertyType.STRING);

    PropertyDefinitionTemplate pdt5 = manager.createPropertyDefinitionTemplate();
    pdt5.setName("users");
    pdt5.setRequiredType(PropertyType.STRING);


    ntt.getPropertyDefinitionTemplates().add(pdt);
    ntt.getPropertyDefinitionTemplates().add(pdt2);
    ntt.getPropertyDefinitionTemplates().add(pdt4);
    ntt.getPropertyDefinitionTemplates().add(pdt5);

    try {
      manager.registerNodeType(ntt, false);
    } catch (NodeTypeExistsException e) {
      LOG.error("Node Exists", e);
    }
  }

  @NotNull
  private List<IRepositoryObject> getRepoObjects(
      @NotNull Node files, @NotNull String fileType, String username, @NotNull List<String> roles) {
    Acl2 acl2 = new Acl2(files);
    acl2.setAdminRoles(userService.getAdminRoles());

    List<IRepositoryObject> repoObjects = new ArrayList<IRepositoryObject>();
    Iterable<Node> objects = null;
    NodeIterator n = null;
    try {
      n = files.getNodes();


      while (n.hasNext()) {
        Node node = n.nextNode();
        String nodetype = node.getPrimaryNodeType().getName();
        String nodename = node.getName();
        String nodepath = node.getPath();
        
        objects = JcrUtils.getChildNodes(node);
        String s = node.getPrimaryNodeType().getName();
        LOG.error("NODO: " + nodename + " - " + nodetype + " - " + nodepath + " - " + s);
        if (acl2.canRead(node, username, roles)) {
          List<AclMethod> acls = acl2.getMethods(node, username, roles);
          if (node.getPrimaryNodeType().getName().equals("nt:file")) {
            if (StringUtils.isNotEmpty(fileType) && !node.getName().endsWith(fileType)) {
              continue;
            }
            String extension = ".saiku"; //file.getName().getExtension();

            repoObjects
                .add(new RepositoryFileObject(node.getName(), "#" + node.getPath(), extension, node.getPath(), acls));
          }
          if (node.getPrimaryNodeType().getName().equals("nt:folder")) {
            repoObjects.add(new RepositoryFolderObject(node.getName(), "#" + node.getPath(), node.getPath(), acls,
                getRepoObjects(node, fileType, username, roles)));
          }
          Collections.sort(repoObjects, new Comparator<IRepositoryObject>() {

            public int compare(@NotNull IRepositoryObject o1, @NotNull IRepositoryObject o2) {
              if (o1.getType().equals(IRepositoryObject.Type.FOLDER) && o2.getType()
                                                                          .equals(IRepositoryObject.Type.FILE)) {
                return -1;
              }
              if (o1.getType().equals(IRepositoryObject.Type.FILE) && o2.getType()
                                                                        .equals(IRepositoryObject.Type.FOLDER)) {
                return 1;
              }
              return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());

            }

          });
        }
        for (Node file : objects) {
          //if (!file.isHidden()) {
          Acl2 acl3 = new Acl2(files);
          acl3.setAdminRoles(userService.getAdminRoles());
          if (acl3.canRead(file, username, roles)) {
            String filename = file.getName();
            String relativePath = file.getPath(); //repo.getName().getRelativeName(file.getName());


            //if ( acl.canRead(relativePath,username, roles) ) {
            List<AclMethod> acls = acl3.getMethods(file, username, roles);

            String s2 = file.getPrimaryNodeType().getName();

            if (file.getPrimaryNodeType().getName().equals("nt:saikufiles")) {
              if (StringUtils.isNotEmpty(fileType) && !filename.endsWith(fileType)) {
                continue;
              }
              String extension = ".saiku"; //file.getName().getExtension();

              repoObjects.add(new RepositoryFileObject(filename, "#" + relativePath, extension, relativePath, acls));
            }
            if (file.getPrimaryNodeType().getName().equals("nt:folder")) {
              //repoObjects.add(new RepositoryFolderObject(filename, "#" + relativePath, relativePath, acls,
              // getRepoObjects(file, fileType, username, roles)));
            }
            Collections.sort(repoObjects, new Comparator<IRepositoryObject>() {

              public int compare(@NotNull IRepositoryObject o1, @NotNull IRepositoryObject o2) {
                if (o1.getType().equals(IRepositoryObject.Type.FOLDER) && o2.getType()
                                                                            .equals(IRepositoryObject.Type.FILE)) {
                  return -1;
                }
                if (o1.getType().equals(IRepositoryObject.Type.FILE) && o2.getType()
                                                                          .equals(IRepositoryObject.Type.FOLDER)) {
                  return 1;
                }
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());

              }

            });
          }
          //}
          //}
        }

      }
    } catch (RepositoryException e) {
      LOG.error("Error processing repo objects", e);
    }
    return repoObjects;
  }

}
