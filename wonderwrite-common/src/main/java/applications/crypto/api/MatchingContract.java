package applications.crypto.api;

import applications.crypto.features.matching.MatchingCommand;
import applications.crypto.features.matching.MatchingResult;

public interface MatchingContract {

    MatchingResult matches(MatchingCommand command);
}
