import java.util.HashMap;
import java.util.Map;

public class UserData {
    private String id; // this should also be the Key for the allUserData HashMap
    private String name;
    private HashMap<Object, Object> data;

    public UserData(String id) {
        this(id, null, null);
    }

    public UserData(String id, String name, HashMap<Object, Object> data) {
        this.id = id;
        this.name = name;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Avoid use of this method where possible; other accessor methods to limit access to data should be implemented in
     * the UserData class instead
     * @return Hashmap representing the user's data
     */
    public HashMap<Object, Object> getDataObject() {
        return data;
    }

    public boolean dataContainsKey (Object key) {
        return data.containsKey(key);
    }

    public Object getData (Object key) {
        return data.get(key);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setData(Object key, Object value){
        data.put(key, value);
    }

    public String userStatsToString() {
        StringBuilder sb = new StringBuilder("Stats for " + name + "\n");
        for (Map.Entry<Object,Object> entry : data.entrySet()) {
            sb.append("- " + entry.getKey() + ": " + entry.getValue() + "\n");
        }
        return sb.toString();
    }
}
