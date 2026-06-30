--
-- PostgreSQL database dump
--

\restrict Ef7AGsXf8ptZcZLm1jJTk7ENDdWTlMU19inC3HtkMb18GVBI7I9g5Wiy8siaSxQ

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

-- Started on 2026-06-13 14:47:17

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 232 (class 1255 OID 16550)
-- Name: update_updated_at(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 224 (class 1259 OID 16487)
-- Name: component_compat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.component_compat (
    id integer NOT NULL,
    component_id integer NOT NULL,
    updated_at timestamp with time zone DEFAULT now(),
    socket text,
    chipset character varying(30),
    ram_type character varying(10),
    ram_slots smallint,
    ram_max_freq_mhz integer,
    ram_height_mm smallint,
    ram_capacity_gb smallint,
    tdp_w smallint,
    cpu_power_pin character varying(20),
    max_tdp_w smallint,
    cooler_height_mm smallint,
    psu_wattage_w smallint,
    psu_form_factor character varying(10),
    psu_length_mm smallint,
    gpu_power_pin character varying(30),
    psu_efficiency character varying(10),
    form_factor character varying(15),
    pci_version character varying(5),
    m2_slots smallint,
    m2_types text[],
    gpu_chipset character varying(80),
    vram_gb smallint,
    gpu_length_mm smallint,
    gpu_height_mm smallint,
    gpu_slots smallint,
    gpu_tdp_w smallint,
    gpu_req_psu_w smallint,
    gpu_pci_version character varying(5),
    max_gpu_length_mm smallint,
    max_cpu_cooler_height_mm smallint,
    max_psu_length_mm smallint,
    supported_mb_formats text[],
    ssd_interface character varying(10),
    ssd_form_factor character varying(20),
    ssd_capacity_gb integer
);


ALTER TABLE public.component_compat OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 16486)
-- Name: component_compat_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.component_compat_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.component_compat_id_seq OWNER TO postgres;

--
-- TOC entry 5104 (class 0 OID 0)
-- Dependencies: 223
-- Name: component_compat_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.component_compat_id_seq OWNED BY public.component_compat.id;


--
-- TOC entry 220 (class 1259 OID 16437)
-- Name: component_prices; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.component_prices (
    id integer NOT NULL,
    component_id integer NOT NULL,
    store character varying(20) NOT NULL,
    price_rub integer DEFAULT 0 NOT NULL,
    product_url text DEFAULT ''::text,
    in_stock boolean DEFAULT true,
    updated_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.component_prices OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16436)
-- Name: component_prices_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.component_prices_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.component_prices_id_seq OWNER TO postgres;

--
-- TOC entry 5105 (class 0 OID 0)
-- Dependencies: 219
-- Name: component_prices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.component_prices_id_seq OWNED BY public.component_prices.id;


--
-- TOC entry 222 (class 1259 OID 16463)
-- Name: component_specs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.component_specs (
    id integer NOT NULL,
    component_id integer NOT NULL,
    store character varying(20) NOT NULL,
    specs jsonb DEFAULT '{}'::jsonb NOT NULL,
    updated_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.component_specs OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 16462)
-- Name: component_specs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.component_specs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.component_specs_id_seq OWNER TO postgres;

--
-- TOC entry 5106 (class 0 OID 0)
-- Dependencies: 221
-- Name: component_specs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.component_specs_id_seq OWNED BY public.component_specs.id;


--
-- TOC entry 230 (class 1259 OID 16557)
-- Name: components; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.components (
    id integer NOT NULL,
    name text NOT NULL,
    category character varying(50) NOT NULL,
    image_url text DEFAULT ''::text,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);


ALTER TABLE public.components OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 16556)
-- Name: components_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.components_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.components_id_seq OWNER TO postgres;

--
-- TOC entry 5107 (class 0 OID 0)
-- Dependencies: 229
-- Name: components_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.components_id_seq OWNED BY public.components.id;


--
-- TOC entry 228 (class 1259 OID 16535)
-- Name: ref_case_mb_compat; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ref_case_mb_compat (
    case_form_factor text NOT NULL,
    mb_form_factor text NOT NULL,
    compatible boolean DEFAULT true
);


ALTER TABLE public.ref_case_mb_compat OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 16525)
-- Name: ref_pcie_lanes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ref_pcie_lanes (
    socket character varying(20) NOT NULL,
    cpu_pcie_lanes smallint NOT NULL,
    notes text DEFAULT ''::text
);


ALTER TABLE public.ref_pcie_lanes OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 16510)
-- Name: ref_socket_chipset; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ref_socket_chipset (
    id integer NOT NULL,
    socket character varying(20) NOT NULL,
    chipset character varying(30) NOT NULL,
    requires_bios_flash boolean DEFAULT false,
    cpu_gen_min smallint,
    cpu_gen_max smallint,
    notes text DEFAULT ''::text
);


ALTER TABLE public.ref_socket_chipset OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 16509)
-- Name: ref_socket_chipset_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ref_socket_chipset_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.ref_socket_chipset_id_seq OWNER TO postgres;

--
-- TOC entry 5108 (class 0 OID 0)
-- Dependencies: 225
-- Name: ref_socket_chipset_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ref_socket_chipset_id_seq OWNED BY public.ref_socket_chipset.id;


--
-- TOC entry 231 (class 1259 OID 18306)
-- Name: v_components_full; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_components_full AS
 SELECT c.id,
    c.name,
    c.category,
    c.image_url,
    c.updated_at,
    COALESCE(p_cl.price_rub, 0) AS price_citilink,
    COALESCE(p_cl.in_stock, false) AS in_stock_citilink,
    COALESCE(p_cl.product_url, ''::text) AS url_citilink,
    COALESCE(p_dn.price_rub, 0) AS price_dns,
    COALESCE(p_dn.in_stock, false) AS in_stock_dns,
    COALESCE(p_dn.product_url, ''::text) AS url_dns,
    COALESCE(p_rg.price_rub, 0) AS price_regard,
    COALESCE(p_rg.in_stock, false) AS in_stock_regard,
    COALESCE(p_rg.product_url, ''::text) AS url_regard,
    COALESCE(s_cl.specs, '{}'::jsonb) AS specs_citilink,
    COALESCE(s_dn.specs, '{}'::jsonb) AS specs_dns,
    COALESCE(s_rg.specs, '{}'::jsonb) AS specs_regard,
    cc.socket,
    cc.chipset,
    cc.ram_type,
    cc.ram_slots,
    cc.ram_max_freq_mhz,
    cc.ram_height_mm,
    cc.ram_capacity_gb,
    cc.tdp_w,
    cc.cpu_power_pin,
    cc.max_tdp_w,
    cc.cooler_height_mm,
    cc.psu_wattage_w,
    cc.psu_form_factor,
    cc.psu_length_mm,
    cc.psu_efficiency,
    cc.gpu_power_pin,
    cc.form_factor,
    cc.pci_version,
    cc.m2_slots,
    cc.m2_types,
    cc.gpu_chipset,
    cc.vram_gb,
    cc.gpu_length_mm,
    cc.gpu_height_mm,
    cc.gpu_slots,
    cc.gpu_tdp_w,
    cc.gpu_req_psu_w,
    cc.gpu_pci_version,
    cc.max_gpu_length_mm,
    cc.max_cpu_cooler_height_mm,
    cc.max_psu_length_mm,
    cc.supported_mb_formats,
    cc.ssd_interface,
    cc.ssd_form_factor,
    cc.ssd_capacity_gb
   FROM (((((((public.components c
     LEFT JOIN public.component_prices p_cl ON (((p_cl.component_id = c.id) AND ((p_cl.store)::text = 'citilink'::text))))
     LEFT JOIN public.component_prices p_dn ON (((p_dn.component_id = c.id) AND ((p_dn.store)::text = 'dns'::text))))
     LEFT JOIN public.component_prices p_rg ON (((p_rg.component_id = c.id) AND ((p_rg.store)::text = 'regard'::text))))
     LEFT JOIN public.component_specs s_cl ON (((s_cl.component_id = c.id) AND ((s_cl.store)::text = 'citilink'::text))))
     LEFT JOIN public.component_specs s_dn ON (((s_dn.component_id = c.id) AND ((s_dn.store)::text = 'dns'::text))))
     LEFT JOIN public.component_specs s_rg ON (((s_rg.component_id = c.id) AND ((s_rg.store)::text = 'regard'::text))))
     LEFT JOIN public.component_compat cc ON ((cc.component_id = c.id)));


ALTER VIEW public.v_components_full OWNER TO postgres;

--
-- TOC entry 4897 (class 2604 OID 16490)
-- Name: component_compat id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_compat ALTER COLUMN id SET DEFAULT nextval('public.component_compat_id_seq'::regclass);


--
-- TOC entry 4889 (class 2604 OID 16440)
-- Name: component_prices id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_prices ALTER COLUMN id SET DEFAULT nextval('public.component_prices_id_seq'::regclass);


--
-- TOC entry 4894 (class 2604 OID 16466)
-- Name: component_specs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_specs ALTER COLUMN id SET DEFAULT nextval('public.component_specs_id_seq'::regclass);


--
-- TOC entry 4904 (class 2604 OID 16560)
-- Name: components id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.components ALTER COLUMN id SET DEFAULT nextval('public.components_id_seq'::regclass);


--
-- TOC entry 4899 (class 2604 OID 16513)
-- Name: ref_socket_chipset id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ref_socket_chipset ALTER COLUMN id SET DEFAULT nextval('public.ref_socket_chipset_id_seq'::regclass);


--
-- TOC entry 4926 (class 2606 OID 16499)
-- Name: component_compat component_compat_component_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_compat
    ADD CONSTRAINT component_compat_component_id_key UNIQUE (component_id);


--
-- TOC entry 4928 (class 2606 OID 16497)
-- Name: component_compat component_compat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_compat
    ADD CONSTRAINT component_compat_pkey PRIMARY KEY (id);


--
-- TOC entry 4909 (class 2606 OID 16454)
-- Name: component_prices component_prices_component_id_store_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_prices
    ADD CONSTRAINT component_prices_component_id_store_key UNIQUE (component_id, store);


--
-- TOC entry 4911 (class 2606 OID 16452)
-- Name: component_prices component_prices_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_prices
    ADD CONSTRAINT component_prices_pkey PRIMARY KEY (id);


--
-- TOC entry 4918 (class 2606 OID 16478)
-- Name: component_specs component_specs_component_id_store_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_specs
    ADD CONSTRAINT component_specs_component_id_store_key UNIQUE (component_id, store);


--
-- TOC entry 4920 (class 2606 OID 16476)
-- Name: component_specs component_specs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_specs
    ADD CONSTRAINT component_specs_pkey PRIMARY KEY (id);


--
-- TOC entry 4942 (class 2606 OID 16572)
-- Name: components components_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.components
    ADD CONSTRAINT components_name_key UNIQUE (name);


--
-- TOC entry 4944 (class 2606 OID 16570)
-- Name: components components_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.components
    ADD CONSTRAINT components_pkey PRIMARY KEY (id);


--
-- TOC entry 4940 (class 2606 OID 16544)
-- Name: ref_case_mb_compat ref_case_mb_compat_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ref_case_mb_compat
    ADD CONSTRAINT ref_case_mb_compat_pkey PRIMARY KEY (case_form_factor, mb_form_factor);


--
-- TOC entry 4938 (class 2606 OID 16534)
-- Name: ref_pcie_lanes ref_pcie_lanes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ref_pcie_lanes
    ADD CONSTRAINT ref_pcie_lanes_pkey PRIMARY KEY (socket);


--
-- TOC entry 4934 (class 2606 OID 16522)
-- Name: ref_socket_chipset ref_socket_chipset_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ref_socket_chipset
    ADD CONSTRAINT ref_socket_chipset_pkey PRIMARY KEY (id);


--
-- TOC entry 4936 (class 2606 OID 16524)
-- Name: ref_socket_chipset ref_socket_chipset_socket_chipset_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ref_socket_chipset
    ADD CONSTRAINT ref_socket_chipset_socket_chipset_key UNIQUE (socket, chipset);


--
-- TOC entry 4916 (class 2606 OID 18303)
-- Name: component_prices uq_component_store_price; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_prices
    ADD CONSTRAINT uq_component_store_price UNIQUE (component_id, store);


--
-- TOC entry 4924 (class 2606 OID 18305)
-- Name: component_specs uq_component_store_specs; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.component_specs
    ADD CONSTRAINT uq_component_store_specs UNIQUE (component_id, store);


--
-- TOC entry 4929 (class 1259 OID 16506)
-- Name: idx_compat_chipset; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_compat_chipset ON public.component_compat USING btree (chipset);


--
-- TOC entry 4930 (class 1259 OID 16508)
-- Name: idx_compat_form_factor; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_compat_form_factor ON public.component_compat USING btree (form_factor);


--
-- TOC entry 4931 (class 1259 OID 16507)
-- Name: idx_compat_ram_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_compat_ram_type ON public.component_compat USING btree (ram_type);


--
-- TOC entry 4932 (class 1259 OID 16590)
-- Name: idx_compat_socket; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_compat_socket ON public.component_compat USING btree (socket);


--
-- TOC entry 4945 (class 1259 OID 16573)
-- Name: idx_components_category; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_components_category ON public.components USING btree (category);


--
-- TOC entry 4946 (class 1259 OID 16574)
-- Name: idx_components_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_components_name ON public.components USING gin (to_tsvector('russian'::regconfig, name));


--
-- TOC entry 4912 (class 1259 OID 16460)
-- Name: idx_prices_component; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prices_component ON public.component_prices USING btree (component_id);


--
-- TOC entry 4913 (class 1259 OID 16461)
-- Name: idx_prices_store; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prices_store ON public.component_prices USING btree (store);


--
-- TOC entry 4914 (class 1259 OID 18249)
-- Name: idx_prices_store_component; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prices_store_component ON public.component_prices USING btree (store, component_id);


--
-- TOC entry 4921 (class 1259 OID 16484)
-- Name: idx_specs_component; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_specs_component ON public.component_specs USING btree (component_id);


--
-- TOC entry 4922 (class 1259 OID 16485)
-- Name: idx_specs_gin; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_specs_gin ON public.component_specs USING gin (specs);


--
-- TOC entry 4949 (class 2620 OID 16583)
-- Name: component_compat trg_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_updated_at BEFORE UPDATE ON public.component_compat FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();


--
-- TOC entry 4947 (class 2620 OID 16581)
-- Name: component_prices trg_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_updated_at BEFORE UPDATE ON public.component_prices FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();


--
-- TOC entry 4948 (class 2620 OID 16582)
-- Name: component_specs trg_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_updated_at BEFORE UPDATE ON public.component_specs FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();


--
-- TOC entry 4950 (class 2620 OID 16580)
-- Name: components trg_updated_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_updated_at BEFORE UPDATE ON public.components FOR EACH ROW EXECUTE FUNCTION public.update_updated_at();


-- Completed on 2026-06-13 14:47:17

--
-- PostgreSQL database dump complete
--

\unrestrict Ef7AGsXf8ptZcZLm1jJTk7ENDdWTlMU19inC3HtkMb18GVBI7I9g5Wiy8siaSxQ

