package fr.anisekai.server.exceptions.user;

import fr.anisekai.core.annotations.FatalTask;
import fr.anisekai.server.exceptions.EntityNotFoundException;

@FatalTask
public class UserNotFoundException extends EntityNotFoundException {

}
