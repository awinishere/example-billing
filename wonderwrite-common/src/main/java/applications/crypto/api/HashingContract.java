package applications.crypto.api;

import applications.crypto.features.hashing.HashingCommand;
import applications.crypto.features.hashing.HashingResult;

public interface HashingContract {

    HashingResult hash(HashingCommand command);
}
