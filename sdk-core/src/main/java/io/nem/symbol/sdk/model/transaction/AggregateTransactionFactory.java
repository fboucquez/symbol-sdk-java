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
package io.nem.symbol.sdk.model.transaction;

import io.nem.symbol.core.crypto.Hasher;
import io.nem.symbol.core.crypto.Hashes;
import io.nem.symbol.core.crypto.MerkleHashBuilder;
import io.nem.symbol.core.utils.ConvertUtils;
import io.nem.symbol.sdk.infrastructure.BinarySerializationImpl;
import io.nem.symbol.sdk.model.network.NetworkType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

/** Factory of {@link AggregateTransaction} */
public class AggregateTransactionFactory extends TransactionFactory<AggregateTransaction> {

  public static final int COSIGNATURE_SIZE = 104;

  private final String transactionsHash;

  private final List<Transaction> innerTransactions;

  private final List<AggregateTransactionCosignature> cosignatures;

  private AggregateTransactionFactory(
      TransactionType type,
      Deadline deadline,
      NetworkType networkType,
      String transactionsHash,
      List<Transaction> innerTransactions,
      List<AggregateTransactionCosignature> cosignatures) {
    super(type, networkType, deadline);
    // Remove this once rest provides the transactionsHash
    String theTransactionsHash =
        transactionsHash == null ? calculateTransactionsHash(innerTransactions) : transactionsHash;
    Validate.notNull(theTransactionsHash, "transactionsHash must not be null");
    Validate.notNull(innerTransactions, "InnerTransactions must not be null");
    Validate.notNull(cosignatures, "Cosignatures must not be null");
    ConvertUtils.validateIsHexString(theTransactionsHash, 64);
    this.transactionsHash = theTransactionsHash;
    this.innerTransactions = Collections.unmodifiableList(innerTransactions);
    this.cosignatures = new ArrayList<>(cosignatures);
  }

  /**
   * Create an aggregate transaction factory that can be customized.
   *
   * @param type Transaction type.
   * @param networkType Network type.
   * @param deadline the deadline
   * @param transactionsHash Aggregate hash of an aggregate's transactions
   * @param innerTransactions List of inner transactions.
   * @param cosignatures List of transaction cosigners signatures.
   * @return The aggregate transaction factory
   */
  public static AggregateTransactionFactory create(
      TransactionType type,
      NetworkType networkType,
      Deadline deadline,
      String transactionsHash,
      List<Transaction> innerTransactions,
      List<AggregateTransactionCosignature> cosignatures) {
    return new AggregateTransactionFactory(
        type, deadline, networkType, transactionsHash, innerTransactions, cosignatures);
  }

  /**
   * Create an aggregate transaction factory that can be customized.
   *
   * @param type Transaction type.
   * @param networkType Network type.
   * @param deadline The deadeline.
   * @param innerTransactions List of inner transactions.
   * @param cosignatures List of transaction cosigners signatures.
   * @return The aggregate transaction factory
   */
  public static AggregateTransactionFactory create(
      TransactionType type,
      NetworkType networkType,
      Deadline deadline,
      List<Transaction> innerTransactions,
      List<AggregateTransactionCosignature> cosignatures) {
    return create(
        type,
        networkType,
        deadline,
        calculateTransactionsHash(innerTransactions),
        innerTransactions,
        cosignatures);
  }

  /**
   * Create an aggregate complete transaction factory that can be customized.
   *
   * @param networkType The network type.
   * @param deadline The deadline
   * @param innerTransactions The list of inner innerTransactions.
   * @return The aggregate transaction factory
   */
  public static AggregateTransactionFactory createComplete(
      NetworkType networkType, Deadline deadline, List<Transaction> innerTransactions) {
    return create(
        TransactionType.AGGREGATE_COMPLETE,
        networkType,
        deadline,
        innerTransactions,
        new ArrayList<>());
  }

  /**
   * Create an aggregate bonded transaction factory that can be customized.
   *
   * @param networkType The network type.
   * @param deadline deadline.
   * @param innerTransactions The list of inner innerTransactions.
   * @return The aggregate transaction factory
   */
  public static AggregateTransactionFactory createBonded(
      NetworkType networkType, Deadline deadline, List<Transaction> innerTransactions) {
    return create(
        TransactionType.AGGREGATE_BONDED,
        networkType,
        deadline,
        innerTransactions,
        new ArrayList<>());
  }

  /**
   * It generates the hash of the transactions that are going to be included in the {@link
   * AggregateTransaction}
   *
   * @param transactions the inner transaction
   * @return the added transaction hash.
   */
  private static String calculateTransactionsHash(final List<Transaction> transactions) {

    final MerkleHashBuilder transactionsHashBuilder = new MerkleHashBuilder();
    final BinarySerializationImpl transactionSerialization = new BinarySerializationImpl();

    Hasher hasher = Hashes::sha3_256;
    for (final Transaction transaction : transactions) {
      final byte[] bytes = transactionSerialization.serializeEmbedded(transaction);
      byte[] transactionHash = hasher.hash(bytes);
      transactionsHashBuilder.update(transactionHash);
    }

    final byte[] hash = transactionsHashBuilder.getRootHash();
    return ConvertUtils.toHex(hash);
  }

  /**
   * Builder method used to to re-calculate the max fee based on the configured feeMultiplier.
   *
   * <p>Because the factory creates an aggregate transcation, an {@link IllegalArgumentException} is
   * raised. users should not use this method.
   *
   * @param feeMultiplier the fee multiplier greater than 1
   * @return raises a IllegalArgumentException
   */
  public AggregateTransactionFactory calculateMaxFeeFromMultiplier(long feeMultiplier) {
    throw new IllegalArgumentException(
        "calculateMaxFeeFromMultiplier can only be used for non-aggregate transactions.");
  }

  /**
   * Set transaction maxFee using fee multiplier for only aggregate transactions.
   *
   * <p>Use this method once all the current transcation consignatures has been added to the
   * factory.
   *
   * @param feeMultiplier The fee multiplier
   * @param requiredCosignatures Required number of cosignatures
   * @return The aggregate transaction factory
   */
  public AggregateTransactionFactory calculateMaxFeeForAggregate(
      long feeMultiplier, int requiredCosignatures) {
    // Check if current cosignature count is greater than requiredCosignatures.
    int calculatedCosignatures = Math.max(this.cosignatures.size(), requiredCosignatures);
    // Remove current cosignature length and use the calculated one.
    long calculatedSize =
        this.getSize() + (calculatedCosignatures - this.cosignatures.size()) * COSIGNATURE_SIZE;
    return (AggregateTransactionFactory)
        maxFee(BigInteger.valueOf(calculatedSize).multiply(BigInteger.valueOf(feeMultiplier)));
  }

  /**
   * Returns list of innerTransactions included in the aggregate transaction.
   *
   * @return List of innerTransactions included in the aggregate transaction.
   */
  public List<Transaction> getInnerTransactions() {
    return innerTransactions;
  }

  /**
   * Returns list of transaction cosigners signatures.
   *
   * @return List of transaction cosigners signatures.
   */
  public List<AggregateTransactionCosignature> getCosignatures() {
    return cosignatures;
  }

  /**
   * Adds cosignatures to the factory if they have been signed independently
   *
   * @param newCosignatures new cosignatures to add
   * @return this builder.
   */
  public AggregateTransactionFactory addCosignatures(
      AggregateTransactionCosignature... newCosignatures) {
    Validate.notNull(newCosignatures, "newCosignatures is required");
    this.cosignatures.addAll(Arrays.asList(newCosignatures));
    return this;
  }

  /** @return Aggregate hash of an aggregate's transactions */
  public String getTransactionsHash() {
    return transactionsHash;
  }

  @Override
  public AggregateTransaction build() {
    return new AggregateTransaction(this);
  }
}
