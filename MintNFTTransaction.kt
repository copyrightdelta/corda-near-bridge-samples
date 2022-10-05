package com.copyrightdelta.drx.flows.music.splits

import co.paralleluniverse.fibers.Suspendable
import kong.unirest.Unirest
import kong.unirest.json.JSONObject
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService


@StartableByRPC
@StartableByService
@InitiatingFlow
class MintNFTTransaction(
    val title: String,
    val contractId: String,
    val description: String,
    val fundingHash: String
) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        val body = JSONObject()
        body.put("contractId", contractId)
        body.put("copies", 1)
        body.put("royalty", 0)
        body.put("mediaCid", "")
        body.put("description", description)
        body.put("assets", arrayOf(arrayOf("1.png", 1, null)))

        val response = Unirest.post("https://satori.art/v1/api/drx/series")
            .header("content-type", "application/json")
            .header(
                "authorization",
                "Bearer XNjpvfQ8q22eubOaNR_Gw"
            )
            .header(
                "funding-hash",
                fundingHash
            )
            .body(
                body.toString()
            )
            .asJson()
        return response.body.getObject().getString("transactionId")
    }
}
