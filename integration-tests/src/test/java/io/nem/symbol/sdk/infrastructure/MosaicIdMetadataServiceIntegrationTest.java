/*
 * Copyright 2020 NEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nem.symbol.sdk.infrastructure;

import io.nem.symbol.sdk.api.MetadataRepository;
import io.nem.symbol.sdk.api.MetadataSearchCriteria;
import io.nem.symbol.sdk.api.MetadataTransactionService;
import io.nem.symbol.sdk.api.RepositoryFactory;
import io.nem.symbol.sdk.model.account.Account;
import io.nem.symbol.sdk.model.metadata.Metadata;
import io.nem.symbol.sdk.model.mosaic.MosaicId;
import io.nem.symbol.sdk.model.transaction.MosaicMetadataTransaction;
import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Integration tests around mosaic metadata service.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MosaicIdMetadataServiceIntegrationTest extends BaseIntegrationTest {


    @ParameterizedTest
    @EnumSource(RepositoryType.class)
    void setAndUpdateMosaicMetadata(RepositoryType type) {
        Account signerAccount = config().getDefaultAccount();
        Account targetAccount = config().getTestAccount();
        MosaicId targetMosaicId = super.createMosaic(signerAccount, type, BigInteger.ZERO, null);

        BigInteger key = BigInteger.valueOf(RandomUtils.generateRandomInt(100000));

        String originalMessage = "The original message";
        String newMessage = "The new Message";

        RepositoryFactory repositoryFactory = getRepositoryFactory(type);
        MetadataRepository metadataRepository = repositoryFactory.createMetadataRepository();

        MetadataTransactionService service = new MetadataTransactionServiceImpl(repositoryFactory);

        MosaicMetadataTransaction originalTransaction = get(service
            .createMosaicMetadataTransactionFactory(targetAccount.getAddress(), key, originalMessage,
                signerAccount.getAddress(), targetMosaicId)).maxFee(maxFee).build();

        announceAggregateAndValidate(type, originalTransaction, signerAccount);

        assertMetadata(targetMosaicId, key, originalMessage, metadataRepository, signerAccount);

        MosaicMetadataTransaction updateTransaction = get(service
            .createMosaicMetadataTransactionFactory(targetAccount.getAddress(), key, newMessage,
                signerAccount.getAddress(), targetMosaicId)).maxFee(maxFee).build();

        announceAggregateAndValidate(type, updateTransaction, signerAccount);

        assertMetadata(targetMosaicId, key, newMessage, metadataRepository, signerAccount);

    }

    private void assertMetadata(MosaicId targetMosaicId, BigInteger key, String value,
        MetadataRepository metadataRepository, Account signerAccount) {
        MetadataSearchCriteria criteria = new MetadataSearchCriteria().targetId(targetMosaicId).scopedMetadataKey(key)
            .sourceAddress(signerAccount.getAddress());
        Metadata originalMetadata = get(metadataRepository.search(criteria)).getData().get(0);
        Assertions.assertEquals(value, originalMetadata.getValue());
    }

}
