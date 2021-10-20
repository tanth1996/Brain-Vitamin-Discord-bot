import java.util.HashMap;
import java.util.Map;

public class UserData {
    private String id; // this should also be the Key for the HashMap
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

    public HashMap<Object, Object> getData() {
        return data;
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
}
