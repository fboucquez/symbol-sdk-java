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

import io.nem.symbol.sdk.model.Stored;
import io.nem.symbol.sdk.model.account.Address;
import io.nem.symbol.sdk.model.mosaic.MosaicId;
import java.math.BigInteger;
import java.util.Optional;

/** It holds information about a hash lock. */
public class HashLockInfo implements Stored {

  /** The stored database id. */
  private final Optional<String> recordId;
  /** Address expressed in hexadecimal base. */
  private final Address ownerAddress;

  /** Mosaic identifier. */
  private final MosaicId mosaicId;

  /**
   * Absolute amount. An amount of 123456789 (absolute) for a mosaic with divisibility 6 means
   * 123.456789 (relative).
   */
  private final BigInteger amount;

  /** Height of the blockchain. */
  private final BigInteger endHeight;

  /** A number that indicates the status. */
  private final Integer status;

  /** Get hash */
  private final String hash;

  public HashLockInfo(
      Optional<String> recordId,
      Address ownerAddress,
      MosaicId mosaicId,
      BigInteger amount,
      BigInteger endHeight,
      Integer status,
      String hash) {
    this.recordId = recordId;
    this.ownerAddress = ownerAddress;
    this.mosaicId = mosaicId;
    this.amount = amount;
    this.endHeight = endHeight;
    this.status = status;
    this.hash = hash;
  }

  @Override
  public Optional<String> getRecordId() {
    return this.recordId;
  }

  public Address getOwnerAddress() {
    return ownerAddress;
  }

  public MosaicId getMosaicId() {
    return mosaicId;
  }

  public BigInteger getAmount() {
    return amount;
  }

  public BigInteger getEndHeight() {
    return endHeight;
  }

  public Integer getStatus() {
    return status;
  }

  public String getHash() {
    return hash;
  }
}
