/*
© Жиляков Д.Э., 2026. Все права защищены.
*/

package com.example.sborkapc.network

import com.example.sborkapc.BuildConfig
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class Component(
    val id: Long = 0,
    val name: String = "",
    val category: String = "",

    @SerializedName(value = "priceCitilink", alternate = ["price_citilink"])
    val price: String = "Нет в наличии",

    @SerializedName(value = "priceRegard", alternate = ["price_regard"])
    val priceRegard: String = "Нет в наличии",

    @SerializedName(value = "priceDNS", alternate = ["price_dns"])
    val priceDns: String = "Нет в наличии",

    @SerializedName(value = "imageUrl", alternate = ["image_url"])
    val imageUrl: String = "",

    @SerializedName(value = "productUrl", alternate = ["url_citilink"])
    val productUrl: String = "",

    @SerializedName(value = "productUrlRegard", alternate = ["url_regard"])
    val productUrlRegard: String = "",

    @SerializedName(value = "productUrlDNS", alternate = ["url_dns"])
    val productUrlDns: String = "",

    val socket: String = "---",
    val chipset: String = "---",
    @SerializedName(value = "ramType", alternate = ["ram_type"])
    val ramType: String = "---",
    @SerializedName(value = "ramSlots", alternate = ["ram_slots"])
    val ramSlots: Int = 0,
    @SerializedName(value = "ramMaxFreq", alternate = ["ram_max_freq_mhz"])
    val ramMaxFreq: Int = 0,
    @SerializedName(value = "ramHeight", alternate = ["ram_height_mm"])
    val ramHeight: Int = 0,
    @SerializedName(value = "ramCapacity", alternate = ["ram_capacity_gb"])
    val ramCapacity: Int = 0,
    @SerializedName(value = "formFactor", alternate = ["form_factor"])
    val formFactor: String = "---",
    @SerializedName(value = "tdp", alternate = ["tdp_w"])
    val tdp: Int = 0,
    @SerializedName(value = "gpuTdp", alternate = ["gpu_tdp_w"])
    val gpuTdp: Int = 0,
    @SerializedName(value = "gpuReqPsu", alternate = ["gpu_req_psu_w"])
    val gpuReqPsu: Int = 0,
    @SerializedName(value = "psuWattage", alternate = ["psu_wattage_w"])
    val psuWattage: Int = 0,
    @SerializedName(value = "gpuLength", alternate = ["gpu_length_mm"])
    val gpuLength: Int = 0,
    @SerializedName(value = "gpuHeight", alternate = ["gpu_height_mm"])
    val gpuHeight: Int = 0,
    @SerializedName(value = "gpuSlots", alternate = ["gpu_slots"])
    val gpuSlots: Int = 0,
    @SerializedName(value = "vram", alternate = ["vram_gb"])
    val vram: Int = 0,
    @SerializedName(value = "gpuChipset", alternate = ["gpu_chipset"])
    val gpuChipset: String = "---",
    @SerializedName(value = "gpuPowerPin", alternate = ["gpu_power_pin"])
    val gpuPowerPin: String = "---",
    @SerializedName(value = "gpuPciVersion", alternate = ["gpu_pci_version"])
    val gpuPciVersion: String = "---",
    @SerializedName(value = "cpuPowerPin", alternate = ["cpu_power_pin"])
    val cpuPowerPin: String = "---",
    @SerializedName(value = "maxGpuLength", alternate = ["max_gpu_length_mm"])
    val maxGpuLength: Int = 0,
    @SerializedName(value = "maxCpuCoolerHeight", alternate = ["max_cpu_cooler_height_mm"])
    val maxCpuCoolerHeight: Int = 0,
    @SerializedName(value = "coolerHeight", alternate = ["cooler_height_mm"])
    val coolerHeight: Int = 0,
    @SerializedName(value = "maxTdp", alternate = ["max_tdp_w"])
    val maxTdp: Int = 0,
    @SerializedName(value = "psuFormFactor", alternate = ["psu_form_factor"])
    val psuFormFactor: String = "---",
    @SerializedName(value = "psuLength", alternate = ["psu_length_mm"])
    val psuLength: Int = 0,
    @SerializedName(value = "psuEfficiency", alternate = ["psu_efficiency"])
    val psuEfficiency: String = "---",
    @SerializedName(value = "pciVersion", alternate = ["pci_version"])
    val pciVersion: String = "---",
    @SerializedName(value = "m2Slots", alternate = ["m2_slots"])
    val m2Slots: Int = 0,
    @SerializedName(value = "m2Types", alternate = ["m2_types"])
    val m2Types: List<String> = emptyList(),
    @SerializedName(value = "ssdInterface", alternate = ["ssd_interface"])
    val ssdInterface: String = "---",
    @SerializedName(value = "ssdFormFactor", alternate = ["ssd_form_factor"])
    val ssdFormFactor: String = "---",
    @SerializedName(value = "ssdCapacityGb", alternate = ["ssd_capacity_gb"])
    val ssdCapacityGb: Int = 0,

    val specs: Map<String, Any>? = null,
    
    @SerializedName(value = "specsCitilink", alternate = ["specs_citilink"])
    val specsCitilink: Map<String, Any>? = null,
    
    @SerializedName(value = "specsRegard", alternate = ["specs_regard"])
    val specsRegard: Map<String, Any>? = null,

    @SerializedName(value = "specsDNS", alternate = ["specs_dns"])
    val specsDns: Map<String, Any>? = null,
)

data class ComponentsResponse(
    val category: String = "",
    val count: Int = 0,
    val components: List<Component> = emptyList()
)

data class Issue(
    val code: String = "",
    val title: String = "",
    val detail: String = "",
    val field: String = ""
)

data class ValidationResponse(
    val status: String = "OK",
    val critical: List<Issue> = emptyList(),
    val warning: List<Issue> = emptyList(),
    val advisory: List<Issue> = emptyList(),
    val summary: Map<String, Any>? = null
)

interface ApiService {
    @GET("components")
    suspend fun getComponents(
        @Query("category") category: String
    ): ComponentsResponse

    @GET("compatibility-check")
    suspend fun checkCompatibility(
        @Query("cpu_id")    cpuId: Long?,
        @Query("mb_id")     mbId: Long?,
        @Query("gpu_id")    gpuIds: List<Long>?,
        @Query("ram_id")    ramIds: List<Long>?,
        @Query("psu_id")    psuId: Long?,
        @Query("case_id")   caseId: Long?,
        @Query("cooler_id") coolerId: Long?,
        @Query("ssd_id")    ssdIds: List<Long>?
    ): ValidationResponse
}

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    val api: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .setLenient()
                        .serializeNulls()
                        .create()
                )
            )
            .build()
            .create(ApiService::class.java)
    }
}