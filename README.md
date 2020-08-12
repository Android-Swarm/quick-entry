## Attribution
Icons used in this application belongs to the respective creators. 
- The blue person icon is made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
- The door icon is taken from Singapore government's SafeEntry website

This project is **purely educational and personal**. The icons are used merely to reduce the need to create custom icons.

## Application Features
### Locations in List
Users are able to navigate to the SafeEntry website by clicking on an item in the list. There is also a search bar to query locations by the name.
Some features in this use case include

##### Location Sorting
The items in the list are sorted according to the distance from the user's current location to the actual location. As user moves, the list is updated dynamically.

##### Checked-In Location Tracking
Locations that a user has checked-in will be indicated with the door icon, and will be placed on the top of the list.

##### Location Details Personalization
Long-clicking an item in the list will display more information about the location, including a Google Map of the position. Here, user can also assign an alias for a particular location.

<img src="https://github.com/Android-Swarm/quick-entry/blob/resource/final_overall.gif" width= "300" height= "500" title="Overall use of the application" />

### One-Time Scanning
User only needs to scan a location once and the required data will be saved to the application. Moreover, this data is also replicated to a common online database, allowing users to collaborate in populating the location. Locations from the common database that are not in a user's device will be automatically added to the user's device when there is an internet connection.

<img src="https://github.com/Android-Swarm/quick-entry/blob/resource/final_scan_new.gif" width= "300" height= "500" title="Scanning QR code saves the location to the list" />

### Save & Generate NRIC Barcode 
Users can save their NRIC barcode by scanning it once using the application. To ensure security, the value of the barcode is encrypted using AES (Advance Encryption Standard).

<img src="https://github.com/Android-Swarm/quick-entry/blob/resource/final_nric_barcode_scanning.gif" width= "300" height= "500" title="NRIC is saved securely" />

## Background
SafeEntry is a contact tracing website that Singapore government implemented during the COVID-19 outbreak. People need to check-in to a location before they enter a location and check-out from it when they exit. To access this website, SafeEntry QR codes are placed around the location. To sum up, the procedure is as follows:
1. Visitor scans the QR code and will be brought to the website
2. Visitor enters their identity and checks-in to the location
3. When finished, visitor rescans the QR code
4. Visitor checks-out to the location (identity can be automatically saved)

Besides scanning QR code, visitors can also use their NRIC for check-in and check-out.

## Issues
These are some issues that are encountered with the following procedure
1. Scanning QR code is a tedious and time consuming
2. Crowding in a particular QR code spot especially when there are a lot of people going to the same location at the same time
3. In the worst case, the QR code paper is damaged and becomes not scannable

## Offered Solution
This application provides workaround for the aforementioned issues.
- A SafeEntry QR code only needs to be scanned once, and all users will have access to that location. Further check-in/check-out can be done from clicking item in the list, hence, no need to crowd a particular spot (See the feature "One-Time Scanning")
- Users know which location has not been checked-out, reducing memory load and helps user to properly contribute in contact tracing (See the feature "Checked-In Location Tracking")
- In case of no network connection, user can generate their saved NRIC barcode (See the feature "Save & Generate NRIC Barcode")
