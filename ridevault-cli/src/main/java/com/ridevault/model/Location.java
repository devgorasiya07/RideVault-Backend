package com.ridevault.model;

public enum Location {
    
    // --- MAJOR IT PARKS & UNIVERSITIES ---
    INFOCITY("Infocity IT Park", 23.1885, 72.6285),
    TCS_GARIMA_PARK("TCS Garima Park", 23.1856, 72.6258),
    GIFT_CITY("GIFT City", 23.1610, 72.6823),
    DA_IICT("DA-IICT Campus", 23.1883, 72.6280),
    NIFT("NIFT Gandhinagar", 23.1905, 72.6288),
    GNLU("Gujarat National Law University", 23.1585, 72.6415),
    PDEU("Pandit Deendayal Energy University", 23.1557, 72.6625),
    NID("National Institute of Design (Sec 26)", 23.2420, 72.6780),
    IHM("Institute of Hotel Management", 23.2015, 72.6295),

    // --- GOVERNMENT & LANDMARKS ---
    VIDHAN_SABHA("Vidhan Sabha / Sachivalaya", 23.2156, 72.6369),
    AKSHARDHAM("Akshardham Temple", 23.2295, 72.6739),
    MAHATMA_MANDIR("Mahatma Mandir Convention Centre", 23.2383, 72.6393),
    DANDI_KUTIR("Dandi Kutir Museum", 23.2393, 72.6415),
    INDRODA_PARK("Indroda Nature Park", 23.2045, 72.6390),
    SWARNIM_PARK("Swarnim Park", 23.2185, 72.6410),
    PUNIT_VAN("Punit Van Botanical Garden", 23.2255, 72.6750),
    CHILDRENS_PARK("Children's Park (Sec 28)", 23.2455, 72.6660),

    // --- TRANSIT & HOSPITALS ---
    PATHIKASHRAM("Pathikashram ST Bus Depot", 23.2255, 72.6425),
    GANDHINAGAR_RAILWAY("Gandhinagar Capital Railway Station", 23.2215, 72.6385),
    CIVIL_HOSPITAL("Civil Hospital (Sec 12)", 23.2115, 72.6465),
    APOLLO_HOSPITAL("Apollo Hospital (Bhat)", 23.1090, 72.6105),

    // --- MAJOR RESIDENTIAL & COMMERCIAL ZONES ---
    SARGASAN_CROSS_ROAD("Sargasan Cross Road", 23.1925, 72.6185),
    KUDASAN("Kudasan / Urjanagar", 23.1850, 72.6215),
    RAYSAN("Raysan", 23.1740, 72.6300),
    BHAIJIPURA("Bhaijipura", 23.1655, 72.6350),
    KOBA_CIRCLE("Koba Circle", 23.1515, 72.6315),
    GIDC_ESTATE("GIDC Electronics Estate (Sec 25)", 23.2360, 72.6900),

    // --- CITY SECTORS & MARKETS ---
    SECTOR_11("Sector 11 (City Mall)", 23.2105, 72.6520),
    SECTOR_16("Sector 16 (Khadi Bhavan)", 23.2205, 72.6480),
    SECTOR_21("Sector 21 Market", 23.2245, 72.6560),
    SECTOR_20("Sector 20 (BAPS Mandir)", 23.2300, 72.6650),
    SECTOR_28("Sector 28 (Garden)", 23.2450, 72.6655),

    // --- MAJOR TRAFFIC CIRCLES (CH & GH ROADS) ---
    CH_0_CIRCLE("CH-0 Circle", 23.1850, 72.6420),
    GH_0_CIRCLE("GH-0 Circle", 23.1780, 72.6450),
    CH_3_CIRCLE("CH-3 Circle", 23.2085, 72.6450),
    GH_3_CIRCLE("GH-3 Circle", 23.2010, 72.6480),
    CH_5_CIRCLE("CH-5 Circle", 23.2325, 72.6480),
    GH_5_CIRCLE("GH-5 Circle", 23.2280, 72.6525),
    CH_7_CIRCLE("CH-7 Circle", 23.2500, 72.6520),
    GH_7_CIRCLE("GH-7 Circle", 23.2465, 72.6570);

    private final String displayName;
    private final double latitude;
    private final double longitude;

    Location(String displayName, double latitude, double longitude) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getDisplayName() { return displayName; }
    public double getLat() { return latitude; }
    public double getLng() { return longitude; }
}