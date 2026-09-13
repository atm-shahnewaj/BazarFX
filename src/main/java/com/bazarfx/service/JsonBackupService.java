package com.bazarfx.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bazarfx.dao.ProductDao;
import com.bazarfx.model.Product;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class JsonBackupService {

    public static boolean exportProductsToJson(String filePath) {
        ProductDao dao = new ProductDao();
        List<Product> products = dao.getAllActiveProducts();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(products, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
