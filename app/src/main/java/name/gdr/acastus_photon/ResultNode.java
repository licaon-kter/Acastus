package name.gdr.acastus_photon;

/**
 * Created by daniel on 7/24/16.
 */
public class ResultNode {

    public String name;
    public String city;
    public String street;
    public String housenumber;
    public String type;
    public double lat;
    public double lon;
    public double distance;

    public void setDistance(Double curLat, Double curLon, GeoLocation geoLocation, Boolean kilometers) {
        this.distance = geoLocation.distance(curLat, lat, curLon, lon, kilometers);
    }

    public String getLabel(Boolean useLocation, Boolean kilometers, Double curLat, Double curLon, GeoLocation geoLocation) {
        String thisLabel = "";
        if (useLocation) {
            if (kilometers){
                thisLabel = name + " : " + distance + " km";
            }else{
                thisLabel = name + " : " + distance + " mi";
            }
        } else {
            thisLabel = name;
        }
        if (this.type != null) {
            thisLabel += " (" + this.type + ")";
        }
        if (this.street != null || this.city != null) {
            thisLabel += "\n";
            if(this.city != null) {
                thisLabel += this.city + " ";
            }
            if(this.street != null) {
                thisLabel += this.street + " "; 
            }
            if(this.housenumber != null) {
                thisLabel += this.housenumber;
            }
        }

        return thisLabel;
    }
}
