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

    /* Format the node for displaying in search results */
    public String getLabel(Boolean useLocation, Boolean kilometers, Double curLat, Double curLon, GeoLocation geoLocation) {
        String thisLabel = "";
        if (useLocation) {
            if (kilometers){
                if(this.name != null && this.name != "") {
                    thisLabel = name + " : " + distance + " km";
                } else {
                    thisLabel = distance + " km";
                }
            }else{
                if(this.name != null && this.name != "") {
                    thisLabel = name + " : " + distance + " mi";
                } else {
                    thisLabel = distance + " mi";
                }
            }
        } else {
            thisLabel = name;
        }
        if (this.type != null) {
            thisLabel += " (" + this.type + ")";
        }
        thisLabel += this.getAddressLabel();

        return thisLabel;
    }

    public String getAddressLabel() {
        String thisLabel = "";
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

    /* Format the result node for history / recent items list */
    public String getRecentsLabel() {
        if(this.name != null && this.name != "") {
            return name;
        }

        return(this.getAddressLabel());
    }

    /* Format the result node for nav app */
    public String getNavLabel() {
        return this.getRecentsLabel();
    }
}
