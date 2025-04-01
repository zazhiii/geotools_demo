package com.zazhi;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;

import org.geotools.styling.Style;
import java.io.File;
import java.util.Iterator;

public class Main {
    public static void main(String[] args) throws Exception{
        // display a data store file chooser dialog for shapefiles
//        LOGGER.info( "Quickstart");
//        LOGGER.config( "Welcome Developers");
//        LOGGER.info("java.util.logging.config.file="+System.getProperty("java.util.logging.config.file"));
//        File file = JFileDataStoreChooser.showOpenFile("shp", null);
//        if (file == null) {
//            return;
//        }
//        LOGGER.config("File selected "+file);

        File file = new File("E:/code_java/myGIS/doc/shapefile/building/天津和平区建筑.shp");


        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();

        // Create a map content and add our shapefile to it
        MapContent map = new MapContent();
        map.setTitle("Quickstart");

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);

        // Now display the map
        JMapFrame.showMap(map);
    }
}