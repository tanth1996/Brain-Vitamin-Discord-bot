package brainvitamin;

import java.sql.SQLException;

public class DuplicatedIdException extends SQLException {
    private String duplicateId;
    DuplicatedIdException(String duplicateId) {
        super("More than one ID exists: " + duplicateId);
        this.duplicateId = duplicateId;
    }

    public String getDuplicateId() {
        return duplicateId;
    }
}
