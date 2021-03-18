package ax.takanoha.simplepoller;

public enum Actions {
    FETCH_SERVICES,
    FETCH_SERVICE,
    CREATE_SERVICE,
    DELETE_SERVICE,
    STORE_POLLING_RESULT;

    public static Actions fromString(String actionString) {
        for (Actions action: Actions.values()) {
            if (action.name().equals(actionString)) return action;
        }
        return null;
    }
}
