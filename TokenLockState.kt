package com.copyrightdelta.drx.contracts.music.splits.states

import com.copyrightdelta.drx.contracts.music.splits.contracts.SplitsContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty

@BelongsToContract(SplitsContract::class)
data class TokenLockState(
    val mintTransactionHash: String,
    val redemptionTransactionHash: String?,
    override var participants: List<AbstractParty>,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState