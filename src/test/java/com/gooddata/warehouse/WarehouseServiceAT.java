/*
 * Copyright (C) 2004-2017, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.warehouse;

import com.gooddata.AbstractGoodDataAT;
import com.gooddata.account.Account;
import com.gooddata.collections.Page;
import com.gooddata.collections.PageRequest;
import com.gooddata.collections.PageableList;
import com.gooddata.project.Environment;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.gooddata.warehouse.WarehouseIdMatcher.hasSameIdAs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

/**
 * Warehouse acceptance tests
 */

public class WarehouseServiceAT extends AbstractGoodDataAT {

    private static final String LOGIN = "john.smith." + UUID.randomUUID() + "@gooddata.com";
    private static final String SCHEMA_NAME = "default";
    private static final String S3_CREDENTIALS_REGION = "us-east";
    private static final String S3_CREDENTIALS_ACCESS_KEY = "WarehouseServiceAt" + RandomStringUtils.randomAlphabetic(5);

    private final String warehouseToken;
    private final WarehouseService service;

    private Warehouse warehouse;
    private Warehouse warehouse2;

    private Account account;
    private WarehouseUser warehouseUser;
    private WarehouseS3Credentials s3Credentials;

    public WarehouseServiceAT() {
        warehouseToken = getProperty("warehouseToken");
        service = gd.getWarehouseService();
    }

    @BeforeClass(groups = "isolated_domain")
    public void initIsolatedDomainGroup() {
        account = gd.getAccountService().createAccount(new Account(LOGIN, "nnPvcGXU7f", "FirstName", "LastName"), getProperty("domain"));
    }

    @Test(groups = "warehouse", dependsOnGroups = "account")
    public void createWarehouse() throws Exception {
        final Warehouse wh = new Warehouse(title, warehouseToken);
        wh.setEnvironment(Environment.TESTING);
        warehouse = service.createWarehouse(wh).get(60, TimeUnit.MINUTES);
        s3Credentials = new WarehouseS3Credentials(S3_CREDENTIALS_REGION, S3_CREDENTIALS_ACCESS_KEY, "secret");
        String jdbc = warehouse.getConnectionUrl();
    }

    @Test(groups = "warehouse", dependsOnMethods = "createWarehouse")
    public void getWarehouse() throws Exception {
        final Warehouse warehouse = service.getWarehouseById(this.warehouse.getId());
        assertThat(warehouse, is(hasSameIdAs(warehouse)));
    }

    @Test(groups = "warehouse", dependsOnMethods = "createWarehouse")
    public void listWarehouses() throws Exception {
        final LinkedList<Warehouse> result = new LinkedList<>();
        Page page = new PageRequest(1000);
        PageableList<Warehouse> warehouses;

        do {
            warehouses = service.listWarehouses(page);
            result.addAll(warehouses);
            page = warehouses.getNextPage();
        } while (warehouses.hasNextPage());

        assertThat(result, hasItem(hasSameIdAs(warehouse)));
    }

    @Test(groups = "warehouse", dependsOnMethods = {"createWarehouse", "listWarehouses"})
    public void shouldPageList() throws Exception {
        final String title = this.title + " second";
        final Warehouse wh = new Warehouse(title, warehouseToken);
        wh.setEnvironment(Environment.TESTING);
        warehouse2 = service.createWarehouse(wh).get();

        final PageableList<Warehouse> firstPage = service.listWarehouses(new PageRequest(1));
        assertThat(firstPage, hasSize(1));

        PageableList<Warehouse> page = service.listWarehouses(firstPage.getNextPage());
        assertThat(page, hasSize(1));
    }

    @Test(groups = "warehouse", dependsOnMethods = "shouldPageList")
    public void shouldReturnNullOnEndOfPaging() throws Exception {
        PageableList<Warehouse> page = service.listWarehouses();
        Page nextPage;
        while ((nextPage = page.getNextPage()) != null) {
            page = service.listWarehouses(nextPage);
        }
    }

    @Test(groups = "warehouse", dependsOnMethods = "createWarehouse")
    public void shouldListUsers() throws Exception {
        final PageableList<WarehouseUser> users = service.listWarehouseUsers(warehouse, new PageRequest(1));
        assertThat(users, hasSize(1));
        assertThat(users.get(0), is(notNullValue()));
        assertThat(users.getNextPage(), is(nullValue()));
    }

    @Test(groups = { "warehouse", "isolated_domain" }, dependsOnMethods = "shouldListUsers")
    public void shouldAddUserToWarehouse() {
        warehouseUser = service.addUserToWarehouse(warehouse, new WarehouseUser(WarehouseUser.ADMIN_ROLE, null, LOGIN)).get(60, TimeUnit.SECONDS);

        assertThat(warehouseUser, is(notNullValue()));
    }

    @Test(groups = { "warehouse", "isolated_domain" }, dependsOnMethods = "shouldAddUserToWarehouse")
    public void shouldRemoveUserFromWarehouse() {
        service.removeUserFromWarehouse(warehouseUser).get(60, TimeUnit.SECONDS);
        warehouseUser = null;
    }

    @Test(groups = "warehouse", dependsOnMethods = "createWarehouse")
    public void listWarehouseSchemas() {
        PageableList<WarehouseSchema> warehouseSchemas = service.listWarehouseSchemas(warehouse);
        assertThat(warehouseSchemas, contains(hasProperty("name", equalTo(SCHEMA_NAME))));
    }

    @Test(groups = "warehouse", dependsOnMethods = "createWarehouse")
    public void getWarehouseSchema() {
        WarehouseSchema warehouseSchema = service.getWarehouseSchemaByName(warehouse, SCHEMA_NAME);
        assertThat(warehouseSchema, is(notNullValue()));
    }

    @Test(groups = "warehouse", dependsOnMethods = "createWarehouse")
    public void getDefaultWarehouseSchema() {
        WarehouseSchema warehouseSchema = service.getDefaultWarehouseSchema(warehouse);
        assertThat(warehouseSchema, is(notNullValue()));
    }

    @Test(groups = { "warehouse", "isolated_domain" }, dependsOnMethods = "createWarehouse")
    public void addS3Credentials() {
        s3Credentials = service.addS3Credentials(warehouse, s3Credentials)
                .get(1, TimeUnit.MINUTES);

        assertThat(s3Credentials, notNullValue());
    }

    @Test(groups = { "warehouse", "isolated_domain" }, dependsOnMethods = "addS3Credentials")
    public void updateS3Credentials() {
        s3Credentials = service.updateS3Credentials(s3Credentials)
                .get(1, TimeUnit.MINUTES);

        assertThat(s3Credentials, notNullValue());
    }

    @Test(groups = { "warehouse", "isolated_domain" }, dependsOnMethods = "updateS3Credentials")
    public void getS3SpecificCredentials() {
        final WarehouseS3Credentials result = service.getWarehouseS3Credentials(warehouse, S3_CREDENTIALS_REGION,
                S3_CREDENTIALS_ACCESS_KEY);

        assertThat(result, notNullValue());
        assertThat(result.getAccessKey(), is(S3_CREDENTIALS_ACCESS_KEY));
    }

    @Test(groups = { "warehouse", "isolated_domain" }, dependsOnMethods = "updateS3Credentials")
    public void listS3Credentials() {
        final PageableList<WarehouseS3Credentials> result = service.listWarehouseS3Credentials(warehouse);

        assertThat(result, notNullValue());
        assertThat(result.size(), is(1));
        assertThat(result.getNextPage(), nullValue());
    }

    @Test(groups = { "warehouse", "isolated_domain" }, dependsOnMethods = { "listS3Credentials", "getS3SpecificCredentials" })
    public void removeS3Credentials() {
        service.removeS3Credentials(s3Credentials).get(1, TimeUnit.MINUTES);
    }

    @Test(dependsOnGroups = "warehouse")
    public void removeWarehouse() throws Exception {
        service.removeWarehouse(warehouse);
        warehouse = null;
        service.removeWarehouse(warehouse2);
        warehouse2 = null;
    }

    @AfterClass
    public void tearDown() throws Exception {
        try {
            if (warehouse != null) {
                service.removeWarehouse(warehouse);
            }
        } catch (Exception ignored) {}
        try {
            if (warehouse2 != null) {
                service.removeWarehouse(warehouse2);
            }
        } catch (Exception ignored) {}
    }

    @AfterClass(groups = "isolated_domain")
    public void tearDownIsolatedDomainGroup() {
        try {
            if (warehouseUser != null) {
                service.removeUserFromWarehouse(warehouseUser);
            }
        }  catch (Exception ignored) {}
        try {
            if (account != null) {
                gd.getAccountService().removeAccount(account);
            }
        } catch (Exception ignored) {}
    }
}
