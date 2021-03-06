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
package io.nem.symbol.sdk.infrastructure.vertx.mappers;

import io.nem.symbol.sdk.infrastructure.TransactionMapper;
import io.nem.symbol.sdk.model.transaction.JsonHelper;
import io.nem.symbol.sdk.model.transaction.Transaction;
import io.nem.symbol.sdk.model.transaction.TransactionFactory;
import io.nem.symbol.sdk.model.transaction.TransactionType;
import java.util.EnumMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Entry point for the transaction mapping. This mapper should support all the known transactions.
 *
 * <p>It's basically a delegator to the specific {@link TransactionMapper} registered in this
 * object.
 */
public class GeneralTransactionMapper implements TransactionMapper {

  private final JsonHelper jsonHelper;

  private final Map<TransactionType, TransactionMapper> transactionMappers =
      new EnumMap<>(TransactionType.class);

  public GeneralTransactionMapper(JsonHelper jsonHelper) {
    this.jsonHelper = jsonHelper;
    Validate.notNull(jsonHelper, "jsonHelper must not be null");
    register(new AddressAliasTransactionMapper(jsonHelper));
    register(new HashLockTransactionMapper(jsonHelper));
    register(new MosaicAddressRestrictionTransactionMapper(jsonHelper));
    register(new MosaicAliasTransactionMapper(jsonHelper));
    register(new MosaicDefinitionTransactionMapper(jsonHelper));
    register(new MosaicGlobalRestrictionTransactionMapper(jsonHelper));
    register(new MosaicSupplyChangeTransactionMapper(jsonHelper));
    register(new MultisigAccountModificationTransactionMapper(jsonHelper));
    register(new NamespaceRegistrationTransactionMapper(jsonHelper));
    register(new SecretLockTransactionMapper(jsonHelper));
    register(new SecretProofTransactionMapper(jsonHelper));
    register(new TransferTransactionMapper(jsonHelper));
    register(new AccountMetadataTransactionMapper(jsonHelper));
    register(new MosaicMetadataTransactionMapper(jsonHelper));
    register(new NamespaceMetadataTransactionMapper(jsonHelper));
    register(new AccountAddressRestrictionTransactionMapper(jsonHelper));
    register(new AccountMosaicRestrictionTransactionMapper(jsonHelper));
    register(new AccountOperationRestrictionTransactionMapper(jsonHelper));
    register(new NodeKeyLinkTransactionMapper(jsonHelper));
    register(new VotingKeyLinkTransactionMapper(jsonHelper));
    register(new VrfKeyLinkTransactionMapper(jsonHelper));
    register(new AccountKeyLinkTransactionMapper(jsonHelper));
    register(new AggregateTransactionMapper(jsonHelper, TransactionType.AGGREGATE_BONDED, this));
    register(new AggregateTransactionMapper(jsonHelper, TransactionType.AGGREGATE_COMPLETE, this));
  }

  private void register(TransactionMapper mapper) {
    if (transactionMappers.put(mapper.getTransactionType(), mapper) != null) {
      throw new IllegalArgumentException(
          "TransactionMapper for type " + mapper.getTransactionType() + " was already registered!");
    }
  }

  @Override
  public TransactionFactory<?> mapToFactoryFromDto(Object transactionInfoDTO) {
    try {
      Validate.notNull(transactionInfoDTO, "transactionInfoDTO must not be null");
      return resolveMapper(transactionInfoDTO).mapToFactoryFromDto(transactionInfoDTO);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Unknown error mapping transaction: "
              + ExceptionUtils.getMessage(e)
              + "\n"
              + jsonHelper.prettyPrint(transactionInfoDTO),
          e);
    }
  }

  @Override
  public Transaction mapFromDto(Object transactionInfoDTO) {
    return this.mapToFactoryFromDto(transactionInfoDTO).build();
  }

  @Override
  public Object mapToDto(Transaction transaction, Boolean embedded) {
    Validate.notNull(transaction, "transaction must not be null");
    return resolveMapper(transaction.getType()).mapToDto(transaction, embedded);
  }

  @Override
  public TransactionType getTransactionType() {
    // All transaction types supported.
    return null;
  }

  private TransactionMapper resolveMapper(Object transactionInfoJson) {
    Integer type = getJsonHelper().getInteger(transactionInfoJson, "transaction", "type");
    if (type == null) {
      throw new IllegalArgumentException(
          "Transaction cannot be mapped, object does not not have transaction type.");
    }
    TransactionType transactionType = TransactionType.rawValueOf(type);
    return resolveMapper(transactionType);
  }

  private TransactionMapper resolveMapper(TransactionType transactionType) {
    TransactionMapper mapper = transactionMappers.get(transactionType);
    if (mapper == null) {
      throw new UnsupportedOperationException("Unimplemented Transaction type " + transactionType);
    }
    return mapper;
  }

  public JsonHelper getJsonHelper() {
    return jsonHelper;
  }
}
