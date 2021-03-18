package ax.takanoha.simplepoller.database;

public enum SqlQuery {
    CREATE_SERVICE_TABLE("create-service-table"),
    FETCH_SERVICES("fetch-services"),
    FETCH_SERVICE("fetch-service"),
    DELETE_SERVICE("delete-service"),
    CREATE_SERVICE("create-service"),
    UPDATE_SERVICE_STATUS("update-service-status");

    public final String name;
    private SqlQuery(String name) {
        this.name = name;
    }
}
