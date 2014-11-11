package org.saiku.service.user;

import org.saiku.database.dto.SaikuUser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IUserManager.
 */
public interface IUserManager {

  @NotNull
  SaikuUser addUser(SaikuUser u);

  boolean deleteUser(SaikuUser u);

  @Nullable
  SaikuUser setUser(SaikuUser u);

  SaikuUser getUser(int id);

  String[] getRoles(SaikuUser u);

  void addRole(SaikuUser u);

  void removeRole(SaikuUser u);

  void removeUser(int username);

  SaikuUser updateUser(SaikuUser u);

  boolean isAdmin();

  List<String> getAdminRoles();

}
