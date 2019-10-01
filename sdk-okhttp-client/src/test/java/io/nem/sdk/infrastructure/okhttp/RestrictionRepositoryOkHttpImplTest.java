/*
 * Copyright 2019 NEM
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nem.sdk.infrastructure.okhttp;

import io.nem.core.utils.MapperUtils;
import io.nem.sdk.model.account.AccountRestrictions;
import io.nem.sdk.model.account.Address;
import io.nem.sdk.model.transaction.AccountRestrictionType;
import io.nem.sdk.openapi.okhttp_gson.model.AccountRestrictionDTO;
import io.nem.sdk.openapi.okhttp_gson.model.AccountRestrictionTypeEnum;
import io.nem.sdk.openapi.okhttp_gson.model.AccountRestrictionsDTO;
import io.nem.sdk.openapi.okhttp_gson.model.AccountRestrictionsInfoDTO;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit Tests for {@link RestrictionRepositoryOkHttpImpl}
 *
 * @author Fernando Boucquez
 */
public class RestrictionRepositoryOkHttpImplTest extends AbstractOkHttpRespositoryTest {

    private RestrictionRepositoryOkHttpImpl repository;


    @BeforeEach
    public void setUp() {
        super.setUp();
        repository = new RestrictionRepositoryOkHttpImpl(apiClientMock);
    }

    @Test
    public void shouldGetAccountRestrictions() throws Exception {
        Address address =
            Address.createFromRawAddress(
                "SBCPGZ3S2SCC3YHBBTYDCUZV4ZZEPHM2KGCP4QXX");

        AccountRestrictionsDTO dto = new AccountRestrictionsDTO();
        dto.setAddress(address.encoded());
        AccountRestrictionDTO restriction = new AccountRestrictionDTO();
        restriction.setRestrictionType(AccountRestrictionTypeEnum.NUMBER_2);
        restriction.setValues(Arrays.asList("9636553580561478212"));
        dto.setRestrictions(Collections.singletonList(restriction));

        AccountRestrictionsInfoDTO info = new AccountRestrictionsInfoDTO();
        info.setAccountRestrictions(dto);
        mockRemoteCall(info);

        AccountRestrictions accountRestrictions = repository
            .getAccountRestrictions(address).toFuture().get();

        Assertions.assertEquals(address, accountRestrictions.getAddress());
        Assertions.assertEquals(1, accountRestrictions.getRestrictions().size());
        Assertions.assertEquals(AccountRestrictionType.ALLOW_INCOMING_MOSAIC,
            accountRestrictions.getRestrictions().get(0).getRestrictionType());
        Assertions.assertEquals(
            Arrays.asList(MapperUtils.toMosaicId("9636553580561478212")),
            accountRestrictions.getRestrictions().get(0).getValues());

    }

    @Test
    public void shouldGetAccountsRestrictionsFromAddresses() throws Exception {
        Address address =
            Address.createFromEncoded(
                "9050B9837EFAB4BBE8A4B9BB32D812F9885C00D8FC1650E142");

        AccountRestrictionsDTO dto = new AccountRestrictionsDTO();
        dto.setAddress(address.encoded());
        AccountRestrictionDTO restriction = new AccountRestrictionDTO();
        restriction.setRestrictionType(AccountRestrictionTypeEnum.NUMBER_1);
        restriction.setValues(Arrays.asList("9050B9837EFAB4BBE8A4B9BB32D812F9885C00D8FC1650E142"));
        dto.setRestrictions(Collections.singletonList(restriction));

        AccountRestrictionsInfoDTO info = new AccountRestrictionsInfoDTO();
        info.setAccountRestrictions(dto);
        mockRemoteCall(Collections.singletonList(info));

        AccountRestrictions accountRestrictions = repository
            .getAccountsRestrictions(Collections.singletonList(address)).toFuture()
            .get().get(0);

        Assertions.assertEquals(address, accountRestrictions.getAddress());
        Assertions.assertEquals(1, accountRestrictions.getRestrictions().size());
        Assertions.assertEquals(AccountRestrictionType.ALLOW_INCOMING_ADDRESS,
            accountRestrictions.getRestrictions().get(0).getRestrictionType());
        Assertions.assertEquals(Collections.singletonList(MapperUtils
                .toAddressFromUnresolved("9050B9837EFAB4BBE8A4B9BB32D812F9885C00D8FC1650E142")),
            accountRestrictions.getRestrictions().get(0).getValues());

    }

    @Override
    public RestrictionRepositoryOkHttpImpl getRepository() {
        return repository;
    }
}
