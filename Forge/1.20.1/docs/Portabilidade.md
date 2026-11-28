# Portabilidade — Simple Storage Manager

**De:** NeoForge 1.21.1 (Minecraft 1.21.1, Java 21)
**Para:** Forge 1.20.1 (Minecraft 1.20.1, Forge 47.4.28, Java 17)
**Data da análise:** 2026-11-27
**Status:** preparação e análise concluídas. **A implementação do mod ainda NÃO foi portada.**

---

## 1. Objetivo e escopo

Este documento registra a **preparação e a análise inicial** da portabilidade do Simple Storage Manager. Nesta etapa:

- **Não** foram adaptadas ou reescritas classes, métodos, registradores, telas, receitas, payloads, assets nem qualquer outra funcionalidade do mod.
- **Não** foi copiado código Java do projeto NeoForge para o projeto Forge.
- **Não** foi alterado o comportamento do mod.
- Foram **inspecionados** os dois projetos e **preparado/validado** apenas o ambiente Gradle do projeto-alvo.
- O projeto-alvo (`D:\Modding\SimpleStorageManager\Forge\1.20.1`) continha, no início, apenas um esqueleto de build com `TODO`s, um `mods.toml` também com `TODO`s e nenhum código-fonte Java.

A categorias usadas neste documento são:

- **[PRESERVAR]** — lógica própria do mod; deve sobreviver à portabilidade com o mesmo significado (podendo exigir apenas ajuste de assinatura/API).
- **[ADAPTAR]** — infraestrutura fornecida pelo loader (NeoForge/Forge) ou API do Minecraft que mudou entre 1.21.1 e 1.20.1; precisa ser substituída.
- **[VERIFICAR]** — ponto cuja forma exata em Forge 1.20.1 não pôde ser confirmada apenas com os arquivos disponíveis; a alternativa provável está indicada, mas deve ser validada na compilação ou em jogo.

A separação entre **código do mod** e **infraestrutura do loader** é explícita nas seções 4, 5 e 6: algoritmos de clique, filtro, ordenação, snapshots, propriedade e layout são do mod; anotações `@Mod`, `@EventBusSubscriber`, `DeferredRegister`, `CustomPacketPayload`, `StreamCodec`, `Capabilities`, `MenuType`, eventos e sistema de config são do loader.

---

## 2. Resumo dos projetos analisados

### 2.1 Projeto original (NeoForge 1.21.1)

**Caminho:** `D:\Modding\SimpleStorageManager\NeoForge\1.21.1`

Arquivos de build e configuração:

| Arquivo | Conteúdo relevante |
| --- | --- |
| `build.gradle` | Plugin `net.neoforged.moddev` **2.0.141**; NeoForge **21.1.262**; toolchain **Java 21**; `jar` nomeado `SimpleStorageManager-NeoForge-1.21.1-v${version}.jar`; `processResources` expande `META-INF/neoforge.mods.toml`; `runs.client/server`; `apply from: 'mcreator.gradle'`. |
| `gradle.properties` | `mod_version=1.2.1`; flags Gradle; ranges de dependências (Sophisticated Core/Storage/Backpacks, Storage Drawers, EMI, JEI, REI). |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle **9.9.0**. |
| `settings.gradle` | `foojay-resolver-convention` 1.0.0; repositórios plugin. |
| `mcreator.gradle`, `simplestoragemanager.mcreator` | `mcreator.gradle` está vazio (no-op); o `.mcreator` é apenas metadado de elementos — **não participa do build Gradle**. |
| `src/main/resources/META-INF/neoforge.mods.toml` | `loaderVersion="[4,)"`; `versionRange="[21.1.262,21.2)"`; `minecraft [1.21.1]`; EMI e REI como `type="optional"` (JEI **não** declarado como dependência opcional). |
| `src/main/resources/META-INF/accesstransformer.cfg` | Vazio (somente comentários). |
| `src/main/resources/pack.mcmeta` | `pack_format: 48`. |

Fontes: **72 arquivos Java** em `src/main/java/net/interplay/simplestoragemanager/`, organizados em:

- `init/` — `SimplestoragemanagerMod{Items,Menus,Recipes,Screens,Tabs}.java` (registros).
- `SimplestoragemanagerMod.java` — bootstrap, catálogo de mensagens, tick do servidor.
- `network/` — 16 classes de payload (ver §5.2).
- `data/` — `TerminalDataComponents.java`, `TerminalConnection.java`, `TerminalConnectionLogic.java`.
- `world/` — `TerminalSavedData.java`, `TerminalOriginMemory.java`; `world/inventory/` — `StorageManagerGUIMenu.java` (≈2.638 linhas), `GuiSlot.java`, `PlayerArmorGuiSlot.java`, `TerminalUpgradeSlot.java`.
- `client/gui/` — `StorageManagerGUIScreen.java` (≈2.026 linhas), `DimensionalUpgradeSelectionScreen.java`, `TerminalScreenLayout.java` (puro), `TerminalHoverHit.java`.
- `logic/` — 13 classes de regras puras (inserção, extração, drag, alcance, upgrades, busca, filtro, organização, propriedade, escala, títulos).
- `access/` — `InventoryAdapter.java`, `MultiblockDiscovery.java`, `AccessRule.java`, `ConnectionStatus.java`.
- `integration/` — `JeiTerminalIntegration.java`, `ReiTerminalIntegration.java`, `EmiTerminalIntegration.java`, `RecipeViewerPriority.java`, 5 classes de integração Sophisticated/Storage Drawers.
- `config/`, `item/`, `item/inventory/`, `recipe/`, `event/`.

Recursos: `assets/simplestoragemanager/lang/{en_us,pt_br}.json`, modelos de item, texturas em `textures/screens/` (ver §5.12), cinco recipes em `data/simplestoragemanager/recipe/`.

Documentação auxiliar do próprio mod: `docs/resumo.md` (comportamento detalhado, decisões e matrizes de teste) — usada como apoio, mas **todas as conclusões abaixo foram conferidas nos arquivos reais**.

**Dependências `compileOnly` do original** (nenhuma incorporada ao JAR):

| Mod | Coordenada 1.21.1 | ID de projeto usado no Maven da Modrinth |
| --- | --- | --- |
| Sophisticated Core | `maven.modrinth:nmoqTijg:[1.21.1-1.5.17.2426,1.21.1-1.5.18)` | `nmoqTijg` |
| Sophisticated Storage | `maven.modrinth:hMlaZH8f:[1.21.1-1.6.9.2199,1.21.1-1.6.10)` | `hMlaZH8f` |
| Sophisticated Backpacks | `maven.modrinth:TyCTlI4b:[1.21.1-3.26.14.2222,1.21.1-3.26.15)` | `TyCTlI4b` |
| Storage Drawers | `maven.modrinth:guitPqEi:[1.21.1-13.11.4,1.21.1-13.12)` com `resolutionStrategy.force` no build NeoForge `px0CCB06` | `guitPqEi` |
| EMI | `dev.emi:emi-neoforge:[1.1.24+1.21.1,):api` | — |
| JEI | `mezz.jei:jei-1.21.1-neoforge:[19.71.0.470,)` | — |
| REI | `me.shedaniel:RoughlyEnoughItems-api-neoforge:[16.0.799,17.0.0)` + JAR completo sem transitivas | — |

Repositórios: Modrinth, Sleeping Town, TerraformersMC, Jared's maven, Shedaniel.

### 2.2 Projeto-alvo (Forge 1.20.1) — estado inicial

**Caminho:** `D:\Modding\SimpleStorageManager\Forge\1.20.1`

| Arquivo | Estado inicial |
| --- | --- |
| `build.gradle` | Esqueleto legacy (`buildscript` + `apply plugin`) com 4 `TODO`s, incluindo `ForgeGradle:TODO`. |
| `gradle.properties` | `minecraft_version=TODO`, `forge_version=TODO`, `loader_version=TODO`, `mappings_version=TODO`. |
| `settings.gradle` | `pluginManagement` apontando para Maven **NeoForge**, foojay **1.0.0**, `rootProject.name` já correto. |
| `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl=TODO`. |
| `src/main/resources/META-INF/mods.toml` | Esqueleto Forge com 6 `TODO`s (`loaderVersion`, `license`, `version`, descrição, faixas de Forge e Minecraft). |
| `.run/Minecraft Client (NeoForge 21.1.262).run.xml` | Resíduo do projeto NeoForge (desatualizado para Forge). |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | **Ausentes**. |
| `src/main/java`, `pack.mcmeta`, `accesstransformer.cfg` | **Ausentes**. |

Total: **15 `TODO`s**. O setup inicial era, portanto, **incompleto e inutilizável** para Forge 1.20.1 — não havia sequer wrapper executável.

---

## 3. Setup Gradle e ambiente

### 3.1 Fonte de verdade

A documentação oficial foi consultada:

- <https://docs.minecraftforge.net/en/1.20.1/gettingstarted/> — pré-requisitos (JDK 17 de 64 bits), MDK, `runClient`/`runServer`, `genIntellijRuns`, `build`.
- <https://docs.minecraftforge.net/en/fg-6.x/> — ForgeGradle 6, `pluginManagement` com `https://maven.minecraftforge.net/`, plugin `net.minecraftforge.gradle` na faixa `[6.0,6.2)`.
- <https://docs.minecraftforge.net/en/fg-6.x/configuration/> — `minecraft { mappings channel: ..., version: ... }`, `copyIdeResources`, access transformers, run configs.
- <https://mcp.thiakil.com/#/search/f_> — índice de mapeamentos MCP útil na futura etapa para localizar equivalentes de nomes de métodos entre versões (ferramenta de consulta, não substitui a compilação).
- **MDK exato do Forge 1.20.1-47.4.28**, publicado no próprio Maven Forge:
  `https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.28/forge-1.20.1-47.4.28-mdk.zip`.
  Foi a fonte das versões exatas de Gradle (8.8), foojay (0.7.0), faixa do ForgeGradle e `pack_format` (15).

### 3.2 Arquivos mantidos, ajustados e criados

| Arquivo | Ação | Detalhe |
| --- | --- | --- |
| `build.gradle` | **Substituído** (era todo `TODO`) | ForgeGradle, toolchain 17, `minecraft { mappings ... runs ... }`, `processResources` com `expand`, `jar` com manifest e `finalizedBy 'reobfJar'`, `publishing`, codificação UTF-8. |
| `gradle.properties` | **Substituído** (TODOs removidos) | Versões reais; `org.gradle.daemon=false`, `parallel=false`, `configuration-cache=false` (precação com FG 6); propriedades de metadados e versões de referência dos mods. |
| `settings.gradle` | **Ajustado** (preservando `mavenLocal`, Maven NeoForge e `rootProject.name`) | Adicionado `maven { url = 'https://maven.minecraftforge.net/' }` em `pluginManagement`; foojay alterado de **1.0.0 → 0.7.0** (versão do MDK 47.4.28). |
| `gradle/wrapper/gradle-wrapper.properties` | **Ajustado** | `distributionUrl=https\://services.gradle.org/distributions/gradle-8.8-bin.zip`. |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` | **Copiados** do projeto original (scripts/JAR genéricos do Gradle) | Eram ausentes; o JAR do wrapper only faz o download da distribuição configurada. |
| `src/main/resources/META-INF/mods.toml` | **Substituído** (TODOs removidos) | Placeholders `${...}` expandidos por `processResources`; dependências opcionais `jei`, `roughlyenoughitems`, `emi` com `mandatory=false`. |
| `src/main/resources/pack.mcmeta` | **Criado** | `pack_format: 15` (formato de recursos do Minecraft 1.20.1) e descrição via `${mod_name}`. |
| `.run/Minecraft Client (NeoForge 21.1.262).run.xml` | **Mantido** (resíduo inofensivo) | Não é usado pelo Gradle; recomenda-se gerar configurações com `genIntellijRuns` na etapa futura e remover este arquivo então. |
| `src/main/java/**` | **Não criado / não modificado** | Nenhuma implementação do mod foi portada. |

### 3.3 Versões exatas configuradas

| Item | Valor |
| --- | --- |
| Minecraft | **1.20.1** (`minecraft_version=1.20.1`, faixa `[1.20.1,1.21)`) |
| Forge | **47.4.28** (`forge_version=47.4.28`, faixa `[47,)`) |
| Loader | **`[47,)`** (`loader_version_range`, usado em `mods.toml`) |
| ForgeGradle | **`[6.0,6.2)`** (faixa oficial do MDK; resolução atual: 6.0.54) |
| Gradle wrapper | **8.8** (`gradle-8.8-bin.zip`) |
| Foojay resolver convention | **0.7.0** (MDK 47.4.28) |
| Java / toolchain | **17** (`JavaLanguageVersion.of(17)`) |
| Mapeamentos | **`official`**, versão **1.20.1** (`mapping_channel`, `mapping_version`) |
| `pack_format` | **15** |
| `mod_version` | **1.2.1** (mesmo valor do projeto original) |
| `mod_group_id` | `net.interplay` |
| Nome do JAR | `SimpleStorageManager-Forge-1.20.1-v${version}.jar` |

**Ambiente local verificado:**

- `JAVA_HOME` apontava para Temurin **21** (`C:\Users\Rodrigo\scoop\apps\temurin21-jdk\21.0.13-9.0`) — inadequado para Forge 1.20.1; a validação foi executada com `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot` (JDK 17.0.21+9, com `javac`). O caminho `C:\Program Files\Java\jdk-17.0.21.9-hotspot` **não existe** nesta máquina.
- Gradle não está no `PATH`; todo comando foi executado via `.\gradlew.bat`.
- Antes desta etapa, o cache de wrapper continha Gradle 8.7, 9.6.0, 9.6.1, 9.8.2 e 9.9.0 — o Gradle 8.8 foi baixado na validação.

### 3.4 Repositórios e dependências

Repositórios configurados no `build.gradle`: Maven Central e Maven Forge (adicionados automaticamente pelo ForgeGradle), Modrinth, Jared's maven (JEI), Shedaniel (REI), TerraformersMC (EMI) e Sleeping Town (preservado do original, não estritamente necessário).

**Nenhuma dependência de mod foi adicionada ao classpath nesta etapa.** As versões abaixo ficam registradas em `gradle.properties` apenas como **referência para a futura portabilidade**, obtidas dos Maven oficiais; a compatibilidade final só será comprovada ao compilar as integrações:

| Mod | Coordenada sugerida para Forge 1.20.1 | Grau de certeza |
| --- | --- | --- |
| JEI | `mezz.jei:jei-1.20.1-forge:15.75.0.243` (e `-forge-api`) | Família 15.x confirmada no Maven BlameJared |
| EMI | `dev.emi:emi-forge:1.1.24+1.20.1:api` | Artefato com classifier `api` confirmado no Maven TerraformersMC |
| REI | `me.shedaniel:RoughlyEnoughItems-api-forge:12.1.786` (+ JAR completo sem transitivas se `DefaultCustomShapedDisplay` for necessário) | **Incerto**: artefato existe, mas o metadata não declara explicitamente MC/loader |
| Sophisticated Core | `maven.modrinth:nmoqTijg:1.20.1-1.5.41.2500` | Confirmado via API Modrinth (`loaders=[forge]`, `game_versions=[1.20.1]`) |
| Sophisticated Storage | `maven.modrinth:hMlaZH8f:1.20.1-1.5.23.2248` | Idem |
| Sophisticated Backpacks | `maven.modrinth:TyCTlI4b:1.20.1-3.26.22.2264` | Idem |
| Storage Drawers | `maven.modrinth:guitPqEi:1.20.1-12.15.1` (build `53npfO5m`) | Idem |

Na futura etapa, usar `compileOnly fg.deobf(...)` para JARs de mods, mantendo a regra do original de **não incorporar** nenhum desses mods ao JAR final.

### 3.5 Validação Gradle executada

Comandos executados no diretório `D:\Modding\SimpleStorageManager\Forge\1.20.1`, com JDK 17 no `PATH`:

1. `.\gradlew.bat --version` — **SUCESSO**. Gradle 8.8, JVM 17.0.21 (Eclipse Adoptium), Windows 11 amd64. A distribuição 8.8 foi baixada durante o comando.
2. `.\gradlew.bat tasks --all --no-daemon` — **BUILD SUCCESSFUL in 13s** (`1 actionable task: 1 executed`). A configuração do projeto imprimiu `Java: 17.0.21, JVM: 17.0.21+9 (Eclipse Adoptium), Arch: amd64` e listou as tarefas esperadas do ForgeGradle (`prepareRunClient`, `runClient`, `runServer`, `genIntellijRuns`, `reobfJar`, `createMcpToSrg`, `createSrgToMcp`, etc.), confirmando que `settings.gradle`, `build.gradle` e `gradle.properties` são válidos e que o plugin ForgeGradle 6.x foi resolvido. Saída completa em `docs/validacao-gradle.txt`. (Uma primeira tentativa falhou apenas porque o diretório `docs` ainda não existia para o `Tee-Object`; a execução foi repetida com sucesso.)
3. `.\gradlew.bat dependencies --configuration compileClasspath --no-daemon` — **BUILD SUCCESSFUL in 59s**. O `compileClasspath` resolveu `net.minecraftforge:forge:1.20.1-47.4.28_mapped_official_1.20.1` e suas transitivas (`net.minecraft:client:1.20.1`, `fmlcore`, `fmlloader`, `eventbus` 6.2.33, mapeamentos `official`, etc.). Durante a resolução, o ForgeGradle executou o Access Transformer processor no JAR injetado e gerou o JAR mapeado oficial 1.20.1. Saída completa em `docs/validacao-dependencias.txt`. (O PowerShell reportou `Exit code: 1` apenas porque os logs INFO do ForgeGradle foram redirecionados via `2>&1` e tratados como registros de erro; o Gradle terminou com `BUILD SUCCESSFUL`.)
4. `.\gradlew.bat build --no-daemon` — **BUILD SUCCESSFUL in 15s** (`6 actionable tasks: 6 executed`; `compileJava NO-SOURCE`, `processResources`, `jar`, `downloadMcpConfig`, `extractSrg`, `createMcpToSrg`, `reobfJar`, `assemble`, `build`). Foi validado apenas o esqueleto de build, **não** código do mod (inexistente no alvo). Gerou `build/libs/SimpleStorageManager-Forge-1.20.1-v1.2.1.jar` e confirmou a expansão correta de `META-INF/mods.toml` (`loaderVersion="[47,)"`, `version="1.2.1"`, `versionRange="[1.20.1,1.21)"`) e de `pack.mcmeta` (`pack_format: 15`). Saída em `docs/validacao-build.txt`.

Bloqueios/observações da validação:

- O `build` completo foi executado, mas **não valida código do mod** porque não há código do mod no projeto-alvo (`compileJava NO-SOURCE`).
- O Gradle 8.8 emitiu aviso: *"Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0"* — esperado para ForgeGradle 6; usar `--warning-mode all` para detalhes. Não bloqueia o setup em 1.20.1.
- `org.gradle.configuration-cache` foi desligado por precaução: ForgeGradle 6 não é confiável com o configuration cache do Gradle 8.8.
- O `JAVA_HOME` do sistema ainda aponta para Java 21; qualquer sessão futura deve definir JAVA_HOME para o JDK 17 antes de executar o wrapper (ou configurar um toolchain no IDE).

---

## 4. Inventário do que deve permanecer

A lógica abaixo pertence ao mod, não ao loader. Na futura portabilidade ela deve ser **preservada semanticamente**; em muitos casos será necessário apenas trocar a chamada de API ao redor dela (assinatura, tipo de dado ou evento), sem alterar a regra.

### 4.1 Regras e algoritmos puros — preservar sem mudança semântica

| Arquivo(s) | Conteúdo do mod a preservar |
| --- | --- |
| `logic/TerminalRange.java` | Raio de 500 blocos, distância ao quadrado a partir do centro do bloco, modo básico vs. upgrades. |
| `logic/UpgradeAccessRules.java` | Nível efetivo (nível 2 vazio não conta), limite de 5 dimensões, regras de distância/dimensão. |
| `logic/UpgradeSlotRules.java` | Posição dos upgrades (3/2/1) e dependência de remoção. |
| `logic/ManualExtractionRules.java`, `logic/ManualInsertionRules.java`, `logic/ShiftDragInsertionRules.java`, `logic/ShiftDragPlanner.java`, `logic/TerminalInsertionRules.java` | Fórmula de extração direita, prioridade de pilhas parciais sobre vazias, plano de arrasto com/sem Shift, limites de item e de slot. |
| `logic/StorageSearch.java`, `logic/TerminalTitleLogic.java`, `logic/TerminalScaleChoice.java`, `logic/TerminalOwnershipRules.java`, `logic/MultiblockBounds.java` | Busca parcial case-insensitive, rolagem/animação de título, tabela SMALL/BIG, autorização por UUID, limite 5×5×5. |
| `data/TerminalConnection.java`, `data/TerminalConnectionLogic.java`, `access/AccessRule.java`, `access/ConnectionStatus.java` | Modelo de conexão (UUID, ordem, membros, avisos, tipo de implementação) e estados. |
| `client/gui/TerminalScreenLayout.java` | Todas as coordenadas das variantes SMALL/BIG — classe 100% pura, sem APIs do loader. |
| `client/gui/TerminalHoverHit.java` | Hit-test compartilhado: remotas → resultado → slots locais; devolve sempre `Result`. |
| `world/TerminalOriginMemory.java` | FIFO de origem em memória de sessão (somente os eventos de limpeza precisam trocar de API). |

### 4.2 Lógica do mod com API ao redor — preservar, ajustar assinatura

| Arquivo(s) | Lógica a manter | Ajuste inevitável ao redor |
| --- | --- | --- |
| `world/inventory/StorageManagerGUIMenu.java` | Índices de slots (upgrades 0–2, armadura 3–6, mão secundária 7, inventário 8–43, resultado 44, grade 45–53 — **contrato para JEI/REI/EMI**); snapshots com `operationSequence`; autoridade do servidor; lock/devolução à origem; limpeza de grade; reavaliação a cada 20 ticks. | `CraftingInput`→`CraftingInventory`, `RecipeHolder`→`Recipe`, leitura/escrita de NBT em vez de componentes, queries de capability, `isSameItemSameComponents`→equivalente 1.20.1, som de equipar. |
| `client/gui/StorageManagerGUIScreen.java` | Layout, scissor, preview otimista, edição de título, dropdowns de filtro/organização, escala da GUI, tooltips manuais. | Overloads de `GuiGraphics` em 1.20.1, `ResourceLocation`, `guiScale`, envio via `SimpleChannel`. |
| `client/gui/DimensionalUpgradeSelectionScreen.java` | Lista de dimensões, seleção até 5, `savePending`, cancelar/salvar. | `renderBackground` hook, `ResourceLocation`, som, envio via `SimpleChannel`. |
| `world/TerminalSavedData.java` | **Formato NBT inteiro**: chaves `Terminals`, `Connections`, `BlockIds`, `FilterConfigs`, `OrganizeConfigs`, `CustomNames`; migração de UUIDs, ordem, dono, cadeado, avisos de substituição. | Assinaturas `load/save(CompoundTag)` sem `HolderLookup.Provider`, `SavedData.Factory` de 2 argumentos, `ResourceLocation`, `Registry.DIMENSION_REGISTRY`, ItemStacks dentro dos filtros. |
| `logic/InventoryFilterLogic.java`, `logic/InventoryOrganizerLogic.java` | Regras de Permitir/Negar e ordenação por Nome/Quantidade/Tipo/Mod; persistência por conexão. | Serialização de ItemStack e tags (`ItemStack.of`, `save(CompoundTag)`), `Registry.ITEM_REGISTRY`. |
| `access/InventoryAdapter.java`, `access/MultiblockDiscovery.java` | Status de disponibilidade (dimensão, chunk carregado, bloco atual), identidade de `IItemHandler` por referência, ligação de baús, limite 5×5×5. | Consultas de capability do Forge com `LazyOptional`; **nenhum chunk é carregado** — requisito comportamental a manter. |
| `integration/*ManualClicks.java`, `StorageDrawersStableItemHandler.java` | Preservar slot clicado, ordem física estável dos drawers, débito medido pela diferença de contagem. | Recompilar contra as versões Forge 1.20.1 desses mods. |
| `recipe/StorageManagerUpgradeRecipe.java` | `isSpecial() = true`, centro do padrão é o terminal, resultado copia o terminal preservando dados, rejeita terminal já avançado. | `CraftingInventory`, serializer 1.20.1, NBT no lugar de componentes. |
| `assets/**`, `data/**` (exceto recipes) | Chaves de tradução, modelos `item/generated`, todas as texturas. | Somente `pack.mcmeta` precisa mudar (`pack_format` 15). |

---

## 5. Inventário do que deve ser substituído ou adaptado

### 5.1 Bootstrap, registros e configuração

| Arquivo / método | Responsabilidade atual | Adaptação necessária | Risco |
| --- | --- | --- | --- |
| `SimplestoragemanagerMod.java` — construtor, `@Mod`, `NeoForge.EVENT_BUS.register(this)`, `registerConfig` | Entrada do mod; recebe `IEventBus` injetado; registra spec SERVER | `@Mod` do Forge, bus via `FMLJavaModLoadingContext.get().getModEventBus()`, `MinecraftForge.EVENT_BUS`, `ModLoadingContext.get().registerConfig(...)`, `ForgeConfigSpec` | **Alto** — nada carrega se o bus estiver errado |
| `SimplestoragemanagerMod.java` — `registerNetworking`, `addNetworkMessage`, `MESSAGES`, record `NetworkMessage` | Catálogo central de 16 mensagens via `RegisterPayloadHandlersEvent` / `PayloadRegistrar.playBidirectional` | Manter a ideia do catálogo, mas trocar por `NetworkRegistry.newSimpleChannel(...)` + `registerMessage` com **direção explícita** | **Alto** |
| `SimplestoragemanagerMod.java` — `tick(ServerTickEvent.Post)` | Varre jogadores e chama `menu.tickConnectionAvailability()` | `TickEvent.ServerTickEvent` (sem fase `.Post`); lógica interna permanece | Médio |
| `config/SimplestoragemanagerConfig.java` | `protectConnectedInventories` (padrão `false`) e `scrollSpeed` (27, 1–500), spec SERVER | `ModConfigSpec` → `ForgeConfigSpec`; registro no construtor do mod | Baixo |
| `init/SimplestoragemanagerModItems.java` — `DeferredRegister.Items`, `DeferredItem` | 4 itens | `DeferredRegister<Item>` + `RegistryObject<Item>` com `ForgeRegistries.ITEMS` | Médio |
| `init/SimplestoragemanagerModItems.java` — `registerCapabilities(RegisterCapabilitiesEvent)` | Expõe `IItemHandler` de 3 slots no `storage_manager` | Forma canônica 1.20.1: `Item#initCapabilities` retornando `ICapabilityProvider` com `LazyOptional<IItemHandler>` **[VERIFICAR]** se `registerItem` existe na versão exata | **Alto** — sem isso não há slots de upgrade |
| `init/SimplestoragemanagerModMenus.java` — `STORAGE_MANAGER_GUI`, `IMenuTypeExtension.create(...)` | Registro do `MenuType` | `new MenuType<>(...)` com factory do Forge; conferir extra-data do `FriendlyByteBuf` (mão principal/secundária) | **Alto** |
| `init/SimplestoragemanagerModMenus.java` — `MenuAccessor.sendMenuStateUpdate` | Sincronização de estado do menu | `PacketDistributor.sendToPlayer/sendToServer` → `SimpleChannel` | Médio |
| `init/SimplestoragemanagerModScreens.java` — imports e `clientLoad` | Registro da tela do menu | `RegisterMenuScreensEvent` do Forge; manter `ScreenAccessor` | Baixo |
| `init/SimplestoragemanagerModRecipes.java` | Registro do serializer | `ForgeRegistries.RECIPE_SERIALIZERS` | Baixo |
| `init/SimplestoragemanagerModTabs.java` | `BuildCreativeModeTabContentsEvent` adiciona 4 itens em `FUNCTIONAL_BLOCKS`; `DeferredRegister` está vazio | Evento do Forge; remover o `DeferredRegister` vazio | Baixo |

**Nota de barramento:** no NeoForge 1.21, `@EventBusSubscriber` escolhe o bus pelo tipo do evento; no Forge 1.20.1 o padrão é o bus de **MOD**. Portanto `TerminalInteractionEvents`, `TerminalOriginMemory`, `RecipeViewerPriority` e `StorageManagerInventoryCapability` precisam de `bus = EventBusSubscriber.Bus.FORGE`. Eventos `FMLCommonSetupEvent` e `RegisterMenuScreensEvent` permanecem no bus de MOD. **[VERIFICAR]** o nome exato do enum na versão fixa de Forge.

### 5.2 Rede — 16 payloads

Padrão comum a **todos** os arquivos de `network/`: `implements CustomPacketPayload`, `Type<T> TYPE`, `StreamCodec<RegistryFriendlyByteBuf,T> STREAM_CODEC`, `handle(T, IPayloadContext)`, `@SubscribeEvent register(FMLCommonSetupEvent)` chamando `SimplestoragemanagerMod.addNetworkMessage(...)`.

| Arquivo | Conteúdo / codecs atuais | Adaptação |
| --- | --- | --- |
| `MenuStateUpdateMessage.java` | `int`, `String`, `Object` (String/boolean/double), validação de tamanho (256 / 8192) | POJO + `encode/decode` em `FriendlyByteBuf`; manter validação; `context.enqueueWork(...).exceptionally(...)` → `enqueueWork` + `disconnect` |
| `DimensionalUpgradeOpenPayload.java` | `RegistryFriendlyByteBuf`, lista de String + boolean | `writeCollection/readCollection` ou laço manual; direção **S→C** |
| `DimensionalUpgradeSavePayload.java` | Lista de String + boolean; escreve `DIMENSION_SELECTION` no componente | NBT no upgrade + `setChanged()`; direção **C→S** |
| `DimensionalUpgradeSaveResultPayload.java` | boolean | **S→C** |
| `TerminalViewPayload.java` (≈235 linhas) | Snapshot completo: `Section`, `VisibleSlot`, `Notice`, `OriginTarget`, `PlayerSlot`, UUIDs, `Component`, ItemStack com contagem separada do codec vanilla | **Preservar toda a lógica de snapshot**; trocar `STACK_CODEC`, `ComponentSerialization`, `BlockPos.STREAM_CODEC`, escrita manual de contagem/capacidade; **S→C** |
| `TerminalViewRequestPayload.java` | String, int, boolean | **C→S** |
| `TerminalConnectionStatusPayload.java` | int, int, `Component`, lista de `VisibleSlot`, boolean; reutiliza codec de `TerminalViewPayload` | **S→C** |
| `TerminalFilterSavePayload.java` | int, int, `List<ItemStack>` via `InventoryFilterLogic.STACK_CODEC` | `ItemStack.save(CompoundTag)` / `ItemStack.of(tag)`; **C→S** |
| `TerminalStorageClickPayload.java` | seção, slot, botão, shift, `List<SlotTarget>`, slots do menu, `operationSequence`, `shiftDragPreviewRequestId` | **Manter ordem dos campos** (autoridade do servidor); **C→S** |
| `TerminalShiftDragPreviewRequestPayload.java` | long + lista de inteiros | **C→S** |
| `TerminalShiftDragPreviewPayload.java` | long, boolean, 3 listas com stacks | **S→C** |
| `TerminalRenamePayload.java` | int + String | **C→S** |
| `TerminalOrganizePayload.java` | int + int | **C→S** |
| `TerminalLockTogglePayload.java` | vazio (`StreamCodec.unit`) | mensagem sem corpo; **C→S** |
| `TerminalClearCraftingPayload.java` | vazio | mensagem sem corpo; **C→S** |
| `TerminalNoticeDismissPayload.java` | String + `BlockPos` | `writeUtf(..., limite)` / `readUtf` + `writeBlockPos/readBlockPos`; **C→S** |

Mapeamentos de API: `CustomPacketPayload.Type` → ID inteiro por `registerMessage`; `StreamCodec`/`ByteBufCodecs` → `encode`/`decode` manuais; `RegistryFriendlyByteBuf` → `FriendlyByteBuf`; `IPayloadContext` → `CustomPayloadEvent.Context`; `context.flow()/player()/connection()` → `side()/getSender()/getConnection()`; `PacketDistributor.sendToPlayer/sendToServer` → `channel.send(PacketDistributor.PLAYER.with(...), msg)` / `channel.sendToServer(msg)`.

**Risco global: Alto.** As defesas autoritativas (`instanceof ServerPlayer`, `player.containerMenu instanceof ...`, limites de string) devem ser preservadas; o "bidirecional" do NeoForge vira escolha explícita de direção.

### 5.3 DataComponents → NBT (maior mudança de semântica)

`data/TerminalDataComponents.java` não existe no Minecraft 1.20.1. Os três componentes e o `DataComponents.CONTAINER` precisam virar um helper NBT estático:

| Componente atual | Tipo | Equivalente 1.20.1 (proposto) |
| --- | --- | --- |
| `TERMINAL_ID` (`terminal_id`) | UUID via Codec/StreamCodec de String | chave NBT no ItemStack (ex.: `SMSTerminalId`) |
| `TERMINAL_ADVANCED` (`advanced`) | boolean | chave NBT boolean (ex.: `SMSAdvanced`), padrão `false` |
| `DIMENSION_SELECTION` (`dimension_selection`) | `List<String>` | `ListTag` de strings (ex.: `SMSDimensions`), padrão vazio |
| `DataComponents.CONTAINER` (3 stacks) | `ComponentItemHandler` | `CompoundTag` com 3 stacks via `save`/`ItemStack.of` |

Pontos exatos de leitura/escrita a adaptar:

| Arquivo / método | Linha(s) aproximadas | Risco |
| --- | --- | --- |
| `item/StorageManagerItem.java` — `getName` | 16 | Médio (nome basic/advanced) |
| `recipe/StorageManagerUpgradeRecipe.java` — `getResultItem`, `assemble`, `isBasicTerminal` | 73, 103, 114 | **Alto** (perder/copiar errado UUID, filtros, cadeado) |
| `event/TerminalInteractionEvents.java` — `openDimensionSelector`, `ensureTerminalId` | 180, 188–192 | **Alto** (ID é a chave do `TerminalSavedData`) |
| `network/DimensionalUpgradeSavePayload.java` — `handle` | 59 | **Alto** (sincronização ao cliente) |
| `world/inventory/StorageManagerGUIMenu.java` — `terminalId()`, `selectedDimensions()`, `applyView` | 215–219, 740, 817 | **Alto** |
| `item/inventory/StorageManagerInventoryCapability.java` — construtor | 31 | **Alto** (3 slots de upgrade) |
| `SimplestoragemanagerMod.java` — registro do `DeferredRegister.DataComponents` | 46 | Médio (remover) |

**Atenção de compatibilidade:** itens e mundos criados em 1.21.1 não serão lidos por uma build 1.20.1 sem camada de conversão. O `TerminalSavedData` usa NBT puro e é majoritariamente compatível, mas os ItemStacks dentro de `FilterConfigs` carregam componentes em 1.21.1. **[NÃO CONFIRMADO]** se se deseja migração de saves — não há migradores no repositório.

### 5.4 Recipe, crafting e JSONs de dados

| Arquivo / método | Adaptação | Risco |
| --- | --- | --- |
| `StorageManagerUpgradeRecipe.java` — `getResultItem(HolderLookup.Provider)`, `matches(CraftingInput, Level)`, `assemble(CraftingInput, HolderLookup.Provider)` | Assinaturas 1.20.1: `getResultItem()`, `matches(CraftingInventory, Level)`, `assemble(CraftingInventory)`; copiar o tag NBT do terminal de entrada | **Alto** |
| `StorageManagerUpgradeRecipe.StorageManagerUpgradeRecipeSerializer` — `MapCodec`, `StreamCodec`, `streamCodec()` | `RecipeSerializer` 1.20.1 usa `Codec<T> codec()`, `fromNetwork(FriendlyByteBuf)`, `toNetwork(FriendlyByteBuf, T)`; nomes exatos de `ShapedRecipePattern` **[VERIFICAR]** | **Alto** |
| `StorageManagerGUIMenu.java` — `slotsChanged` / `quickMoveCraftingResult` (≈2363–2385, 2507–2552) | `craftingGrid` como `CraftingInventory`; `getRecipeFor(...)` retorna receita sem `RecipeHolder`; `setRecipeUsed(ResourceLocation)`; `recipe.assemble(...)`; remover `isItemEnabled(...)` | **Alto** — afeta a bancada usada por JEI/REI/EMI |
| `data/simplestoragemanager/recipe/storage_manager_upgrade.json` | O ingrediente `minecraft:crafter` **não existe no 1.20.1** (bloco de 1.21); substituir por outro item | **Alto** — receita inválida se não corrigido |
| `data/simplestoragemanager/recipe/{storage_manager,infinity_upgrade,dimensional_infinity_upgrade,multidimensional_infinity_upgrade}.json` | Formato de resultado 1.20.5+ (`"result": {"id": ..., "count": ...}`) → 1.20.1 (`"result": "mod:item"`) | Médio |
| `data/.../recipe/*.json` — `"category": "equipment"` | Compatível; manter | Baixo |

### 5.5 Itens, tooltips e capability do terminal

| Arquivo / método | Adaptação | Risco |
| --- | --- | --- |
| `item/StorageManagerItem.java` — `getName`, `shouldCauseReequipAnimation` | `getName` lê NBT em vez de componente; `equals` 1.20.1 compara tag **[VERIFICAR]** | Médio / Baixo |
| `item/{InfinityUpgradeItem,DimensionalInfinityUpgradeItem,MultidimensionalInfinityUpgradeItem}.java` — `appendHoverText` | `TooltipContext` → `Level`; `options.keyShift.isDown()` → `Screen.hasShiftDown()` / `InputConstants`; manter texto traduzido | Médio |
| `item/UpgradeTooltip.java` — `appendDescription` | `font.getSplitter().splitLines(...)` pode mudar; alternativa segura: `font.split(Component, 180)`. Manter largura 180 e texto traduzido | Médio |
| `item/inventory/StorageManagerInventoryCapability.java` — `ComponentItemHandler`, `onItemDropped`, `getSlotLimit`, `isItemValid`, `getStackInSlot` | Implementar handler sobre NBT da stack; `MutableDataComponentHolder` removido; trocar `ItemTossEvent` do NeoForge pelo do Forge (bus FORGE); manter limite 1 e regra "não aceita outro terminal" | **Alto** |

### 5.6 Menu e slots

| Arquivo / trecho | Adaptação | Risco |
| --- | --- | --- |
| `StorageManagerGUIMenu.java` — construtor `(int, Inventory, FriendlyByteBuf)`, `readHand` | Conferir extra-data do `MenuProvider.openMenu`/`MenuType` em 1.20.1; sem o byte da mão o mod não sabe se o terminal está na mão secundária | **Alto** |
| `StorageManagerGUIMenu.java` — `playArmorEquipSound` (≈245) | `Equipable.get(...)` / `getEquipSound().value()` pode não existir; alternativa: `ArmorItem.getEquipSound()` + `playSound`. Manter "tocar som **após** confirmação do servidor" | Médio/Alto |
| `StorageManagerGUIMenu.java` — ~50 usos de `ItemStack.isSameItemSameComponents` | 1.20.1: `isSameItemSameTags` / `isSameItem`; **semântica mais fraca** (NBT em vez de components) — afeta todos os matches de inserção/extração/devolução | **Alto** |
| `StorageManagerGUIMenu.java` — `copyWithCount(n)` (~40 usos) | `copy()` + `setCount(n)` | Médio |
| `StorageManagerGUIMenu.java` — queries `Capabilities.ItemHandler.BLOCK` (≈441, 483) | `CapabilityItemHandler.ITEM_HANDLER_CAPABILITY` + `LazyOptional` | **Alto** |
| `StorageManagerGUIMenu.java` — `ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(...))` (≈2838) | `Registry.DIMENSION_REGISTRY` + `new ResourceLocation(...)` com validação | Baixo |
| `world/inventory/PlayerArmorGuiSlot.java` — `stack.canEquip(...)` | **[VERIFICAR]** em 1.20.1; alternativa `item instanceof ArmorItem` | Médio |
| `world/inventory/TerminalUpgradeSlot.java` | Regras de `UpgradeSlotRules` permanecem; adaptar a capability da stack | **Alto** |
| `world/inventory/GuiSlot.java` — `isActive()` | API estável; manter | Baixo |

### 5.7 Eventos, SavedData e registros

| Arquivo / método | Adaptação | Risco |
| --- | --- | --- |
| `event/TerminalInteractionEvents.java` — `onRightClickBlock`, `onRightClickItem`, `onBreak` | Imports/bus FORGE; `level.getCapability(...).isPresent()`; manter toda a lógica de conectar/desconectar, owner, multiblock e proteção | **Alto** |
| `event/TerminalInteractionEvents.java` — `openTerminal`, `openDimensionSelector`, `ensureTerminalId` | Conferir `openMenu(provider, Consumer<FriendlyByteBuf>)`; `Registry.DIMENSION_REGISTRY`; NBT; `SimpleChannel` | **Alto** |
| `world/TerminalOriginMemory.java` — `onPlayerLoggedOut`, `onServerStopping` | Imports/bus FORGE; lógica de limpeza permanece | Baixo |
| `world/TerminalSavedData.java` — `FACTORY`, `load(CompoundTag, HolderLookup.Provider)`, `save(CompoundTag, HolderLookup.Provider)` | `SavedData.Factory` de 2 argumentos; `load/save(CompoundTag)`; remover `provider` de `InventoryFilterLogic.toTag/fromTag` | Médio |
| `world/TerminalSavedData.java` — `ResourceLocation.parse`, `ResourceKey.create(Registries.DIMENSION, ...)` | `new ResourceLocation(...)` com validação; `Registry.DIMENSION_REGISTRY` | Baixo |
| `logic/InventoryFilterLogic.java` — `STACK_CODEC`, `toTag/fromTag` | `stack.save(new CompoundTag())`, `ItemStack.of(tag)`, codificação manual | Médio |
| `logic/InventoryOrganizerLogic.java` — `TagKey.create(Registries.ITEM, ...)`, `ResourceLocation.parse` | `Registry.ITEM_REGISTRY`, `new ResourceLocation(...)`; conferir tags 1.20.1 | Baixo/Médio |
| Recursos — `pack.mcmeta` 48 → 15; `neoforge.mods.toml` → `mods.toml` | Já aplicado no setup do alvo; na portação, copiar traduções/modelos/texturas e converter apenas os recipes (§5.4) | **Alto** se `pack_format` permanecer 48 |

**Item citado na solicitação e ausente no código:** não há manipulação de encantamentos, dano ou atributos de equipamento; `TagParser` também não aparece em nenhum arquivo. Esses pontos não precisam ser portados.

### 5.8 Capabilities e integrações de terceiros

| Arquivo | Adaptação | Risco |
| --- | --- | --- |
| `access/InventoryAdapter.java` — `resolve`, `implementationKind`, `isChunkLoaded` | `CapabilityItemHandler` + `LazyOptional`; preservar heurística `InvWrapper`/`SidedInvWrapper`, detecção de `MODDED_HANDLER` e **nenhum carregamento de chunk** (**[VERIFICAR]** visibilidade de `getChunkSource().hasChunk` em 1.20.1) | **Alto** |
| `access/MultiblockDiscovery.java` | Mesmas regras de BFS/identidade de handler/ligação de baú, com capability do Forge; **[VERIFICAR]** `ChestBlock.getConnectedDirection` | **Alto** |
| `integration/StorageDrawersManualClicks.java`, `StorageDrawersStableItemHandler.java` | Recompilar contra Storage Drawers **Forge 1.20.1**; `isSameItemSameComponents`→equivalente; `copyWithCount`; manter ordem física estável e débito medido | **Alto** |
| `integration/Sophisticated{Storage,Backpacks,Inventory}ManualClicks.java` | Recompilar contra versões Forge 1.20.1; APIs `insertItemOnlyToSlot`, `getExternalItemHandler`, `getStorageWrapper` podem mudar | **Alto** |
| `integration/JeiTerminalIntegration.java` | API JEI 15.x Forge; `RecipeHolder<T>`→`T`; `getResultItem()` sem provider; **[VERIFICAR]** `getClickableIngredientUnderMouse`/`IClickableIngredientFactory` | **Alto** |
| `integration/ReiTerminalIntegration.java` | API REI 1.20.1/Forge; `RecipeHolder`, `RegistryAccess.EMPTY`, `DefaultCustomShapedDisplay` **[VERIFICAR]** | **Alto** |
| `integration/EmiTerminalIntegration.java` | API EMI Forge 1.20.1; `addScreenBoundsProvider`, `addStackProvider`, `StandardRecipeHandler` **[VERIFICAR]** | **Alto** |
| `integration/RecipeViewerPriority.java` | Eventos `ScreenEvent` do Forge; **revalidar por reflexão** os internos `mezz.jei.common.Internal` e `me.shedaniel.rei.api.client.config.ConfigObject` | **Alto** (pode silenciosamente não suprimir overlays) |

### 5.9 Telas e GUI

| Arquivo / trecho | Adaptação | Risco |
| --- | --- | --- |
| `StorageManagerGUIScreen.java` — `init` / `TerminalScaleChoice` | `options.guiScale().get()` → provavelmente `options.guiScale.get()`; a tabela 0→SMALL, 1/2→BIG, 3/4→SMALL deve permanecer | Médio/Alto |
| `StorageManagerGUIScreen.java` — `renderBg`, `drawRemoteSections`, `drawUnavailable`, `drawReplacementNotice`, `renderPlayer`, `renderLabels`, `renderTooltip` | Overloads de `GuiGraphics.blit/drawString/drawCenteredString/fill/renderTooltip`, `enableScissor`; conferir `renderEntityInInventoryFollowsMouse` e `ClientTooltipPositioner` **[VERIFICAR]** | Médio |
| `StorageManagerGUIScreen.java` — 12+ chamadas `PacketDistributor.sendToServer` | `SimpleChannel.sendToServer` mantendo intenção-only dos payloads | **Alto** |
| `StorageManagerGUIScreen.java` — key mappings (`keyPickItem`, `keyInventory`, `hasShiftDown`) | Conferir `isActiveAndMatches`/`matches` em 1.20.1; comportamento do clique com roda no criativo permanece | Médio |
| `DimensionalUpgradeSelectionScreen.java` | `ResourceLocation.fromNamespaceAndPath`/`tryParse` → `new ResourceLocation(...)` com try/catch; `renderBackground` hook; `SoundEvents.UI_BUTTON_CLICK`/`SimpleSoundInstance`; `SimpleChannel` | Médio |
| `TerminalHoverHit.java`, `TerminalScreenLayout.java` | Nenhuma mudança de lógica prevista | Baixo |

### 5.10 Cuidado A — cores e transparência dos textos

**Onde o mod representa/aplica cores (todos os literais estão em duas classes):**

`client/gui/StorageManagerGUIScreen.java`:

- Constantes: `MENU_TITLE_COLOR = 0xFFFFFFFF` (104), `MENU_OPTION_COLOR = 0xFFfb8f13` (105), `MENU_OPTION_COLOR_UNSELECTED = 0xFF808080` (106).
- `drawClippedTitle` (219): `drawString(..., 0xFFFFFFFF, false)`.
- `sectionMenuLine` (546–572): `Style.EMPTY.withColor(...)` nos títulos e opções dos dropdowns; checkmark `✔` em negrito.
- `init` (661–670): `searchBar.setTextColor(0xFFFFFF)` e `titleEditBox.setTextColor(0xFFFFFF)` (RGB, sem alpha).
- `renderBg` (765): `RenderSystem.setShaderColor(1,1,1,1)`.
- `drawRemoteSections` (845, 925): molduras `0xFFFF9B45` / `0xFFD0D0D0`; mensagem "sem inventários" `0xFFD0D0D0`.
- `drawUnavailable` (934–970): fundo `0xE015120F` (alpha `E0`), faixas e texto `0xFFFF9B45`.
- `drawReplacementNotice` (983–1004): fundo `0xE01B4D27`, moldura/texto `0xFFB5FFBC`, X 7×7.
- `renderLabels` (1067–1079): título, marca `MkII` e nome do dono com `0x404040` (**sem alpha explícito**).
- `render` (1108–1144): tooltips via `ChatFormatting.WHITE/GRAY`, sem int de cor.

`client/gui/DimensionalUpgradeSelectionScreen.java`:

- `renderBackground` (95): `RenderSystem.setShaderColor(1,1,1,1)`.
- Nome da dimensão (119–120): `drawString(..., 0xFFFFFFFF, false)`.
- Dica de seleção (140–141): `drawString(..., 0x404040, false)`.
- Mensagem de erro (152–153): `drawCenteredString(..., 0xFFFF5555)`.

Outros arquivos (`UpgradeTooltip.java` e os três `*UpgradeItem.java`) usam apenas `ChatFormatting.GRAY`.

**Semântica verificada (não presumida):** o código-fonte vanilla disponível em cache para 1.21.1 e 1.20.1 contém o mesmo `Font.adjustColor(int)`: `(color & 0xFC000000) == 0 ? color | 0xFF000000 : color` — ou seja, literais de 6 dígitos como `0x404040` recebem alpha `FF`; literais com alpha explícito são preservados. `Style.withColor(int)` usa `TextColor.fromRgb` e o caminho de renderização lê apenas R/G/B (ignora alpha). `GuiGraphics.fill` usa convenção **ARGB (0xAARRGGBB)**. `EditBox.setTextColor` recebe RGB.

**Risco documentado (motivo: falha na portabilidade Fabric anterior):** se a API de destino não normalizar `0xRRGGBB` para ARGB, os textos `0x404040` ficam com alpha zero/invisíveis. A porta Fabric anterior resolveu isso com decodificações explícitas (por exemplo `0xFF444444`). Na futura portabilidade para Forge 1.20.1:

1. **Não presumir** que a ordem dos canais é igual entre APIs; confirmar explicitamente alpha e conversão/ordem nos pontos acima, inclusive em tooltips de JEI/REI/EMI.
2. Validar explicitamente os literais `0x404040`, `0xE015120F`, `0xE01B4D27`, `0xFFfb8f13`, `0xFFFF5555`.
3. **Verificação visual obrigatória:** título principal, marca `MkII`, nome do dono (escala 0,5), dica da GUI dimensional, mensagem de erro vermelha, títulos das seções, aviso laranja de inventário indisponível, aviso verde de substituição, opções dos dropdowns e texto dos `EditBox`. Confirmar que os fundos translúcidos não ficam opacos nem invisíveis.

### 5.11 Cuidado B — JEI, REI e EMI

**Como está configurado hoje (NeoForge 1.21.1):**

| Viewer | Classe / descoberta | Transferência | Stack sob o mouse | Exposição da receita de upgrade |
| --- | --- | --- | --- | --- |
| JEI | `JeiTerminalIntegration` (`@JeiPlugin`, `IModPlugin`) | `addRecipeTransferHandler`: grade 45–53 como destino `[+]`; inventário do player 8–43 como fontes | `IGuiContainerHandler` → `screen.getHoveredItem/getHoveredSlotRect` | `registerVanillaCategoryExtensions` → `UpgradeRecipeExtension` com `isHandled() = true` (a receita é `isSpecial()`) |
| REI | `ReiTerminalIntegration` (`@REIPluginClient`, `REIClientPlugin`) | `SimpleTransferHandler.create` com `IntRange(45,54)`; fonte implícita `player.getInventory()` | `FocusedStackProvider` prioridade `100.0` → `getHoveredItem` | `registerRecipeFiller` → `DefaultCustomShapedDisplay` (do JAR completo do REI, `transitive=false`) |
| EMI | `EmiTerminalIntegration` (`@EmiEntrypoint`, `EmiPlugin`) | `StandardRecipeHandler`: entradas = grade 45–53 + 36 slots do player; saída = slot 44 | `TerminalStackProvider` com `clickable=false` | Sem registro explícito; depende do processamento de `CraftingRecipe` pelo plugin vanilla do EMI (**[VERIFICAR]** em jogo) |

Índices confirmados no menu: upgrades 0–2, armadura 3–6, mão secundária 7, inventário principal 8–34, hotbar 35–43, resultado 44, grade 45–53.

**Regra a preservar obrigatoriamente:** o item **Storage Manager NÃO pode ser registrado como workstation/catalyst de crafting** em nenhum dos três viewers. Isso está deliberadamente ausente hoje (`JeiTerminalIntegration.java:85-89`, `ReiTerminalIntegration.java:91-95` e nenhum `addWorkstation` no EMI). Na portabilidade Fabric anterior, registrar como workstation fez o **U** listar todas as receitas de crafting do modpack em vez da receita de upgrade.

Prioridade **EMI > REI > JEI** apenas com a GUI do terminal aberta (`RecipeViewerPriority.java`): `EVENT` de abertura/fechamento de tela, reflexão para ligar/desligar overlays, e cancelamento **somente** da tecla que religaria a overlay suprimida (R/U/favoritos não são cancelados). Os nomes internos usados por reflexão são versão-específicos e precisam ser revalidados para 1.20.1.

**Requisito de teste futuro (não concluído nesta etapa):** com cada viewer instalado isoladamente, pressionar **U** sobre um Storage Manager básico deve mostrar **apenas a receita de upgrade esperada** (padrão 3×3 com o terminal no centro → terminal avançado), tanto no inventário do jogador quanto com a GUI aberta; **não** pode listar todas as receitas de crafting do modpack. Validar também R nos itens da grade e o botão `[+]` preenchendo 45–53 a partir de 8–43.

### 5.12 Cuidado C — texturas dos botões e ícones

Dimensões reais dos PNGs verificadas em `assets/simplestoragemanager/textures/screens/`:

| Textura | Dimensão | Onde é usada |
| --- | --- | --- |
| `checkbox.png`, `checkbox_highlighted.png`, `checkbox_selected.png`, `checkbox_selected_highlighted.png` | 20×20 | GUI do Dimensional Infinity Upgrade |
| `bg.png`, `bg_highlighted.png` | 225×20 | linhas da lista dimensional (desenhadas em 225×20, sem esticar) |
| `dimensional_upgrade_gui.png` | 256×256 | fundo da GUI dimensional |
| `unlocked_button.png`, `unlocked_button_highlighted.png`, `locked_button.png`, `locked_button_highlighted.png` | 20×20 | botão de cadeado do Storage Manager |
| `report_button.png`, `report_button_highlighted.png` | 20×20 | alerta de inventário desconectado |
| `cross_button.png`, `cross_button_highlighted.png` | 14×14 | botão X (limpar crafting) e X do aviso verde |
| `organizer.png`, `filter.png` | 18×18 | botões de auto-organização e filtro das seções |
| `slot.png` | 18×18 | slots vazios das seções remotas |
| `scroller.png`, `scroller_disabled.png` | 12×15 | barras de rolagem |
| `storage_manager_gui.png`, `storage_manager_gui_big.png` | 216×230 / 216×342 | fundos SMALL/BIG |

Pontos de renderização/carregamento:

| Elemento | Arquivo / método | Chamada atual |
| --- | --- | --- |
| Checkmarks | `DimensionalUpgradeSelectionScreen.java` — `renderBackground` (110–118) | `graphics.blit(checkbox, x, y, 10, 10, 0, 0, 20, 20, 20, 20)` — 20×20 inteira → 10×10 |
| Cadeado | `StorageManagerGUIScreen.java` — `renderBg` (795–802) | `graphics.blit(lockTexture, x, y, 10, 10, 0, 0, 20, 20, 20, 20)` |
| Limpar receita (X) | `StorageManagerGUIScreen.java` — `renderBg` (790–794) | overload de 9 argumentos: `(tex, x, y, 0, 0, 8, 8, 8, 8)` — a imagem inteira de 14×14 é mapeada em 8×8 porque `textureWidth/Height=8` normaliza o UV para 0..1 |
| Filtro / organização | `StorageManagerGUIScreen.java` — `drawRemoteSections` (910–914) | `graphics.blit(tex, x, y, 10, 10, 0, 0, 18, 18, 18, 18)` |
| Alerta de desconexão | `StorageManagerGUIScreen.java` — `drawUnavailable` (971–978) | `graphics.blit(icon, x, y, 10, 10, 0, 0, 20, 20, 20, 20)` |
| X do aviso verde | `StorageManagerGUIScreen.java` — `drawReplacementNotice` (996–1004) | `graphics.blit(cross, x, y, 7, 7, 0, 0, 14, 14, 14, 14)` |
| ResourceLocations | ambas as telas | `ResourceLocation.fromNamespaceAndPath("simplestoragemanager", "textures/screens/...")` → em 1.20.1, `new ResourceLocation(...)` |

**Regra visual obrigatória para a futura adaptação:** botões de **10×10 pixels** devem usar a **textura inteira reduzida para 10×10**, e **não** recortar uma região 10×10 da textura original. O código atual **cumpre** essa regra: cadeado, alerta, filtro/organização e checkboxes passam `u=0, v=0, uWidth=20/18, vHeight=20/18, textureWidth=20/18, textureHeight=20/18` com destino 10×10. Atenção especial ao X principal: se alguém "corrigir" `textureWidth/Height` de 8 para 14 mantendo `uWidth=8`, passará a recortar o canto 8×8 da imagem de 14×14 — **violação da regra**. O overload de 11 argumentos de `GuiGraphics.blit` existe em 1.20.1 (verificado no JAR de fontes em cache), com a mesma semântica de normalização de UV; não é necessário trocar por `blitSprite`/`drawTexture`. Esses botões são desenhados manualmente, com hit-test manual — não há widgets a migrar (os únicos `Button` reais são Cancelar/Salvar da GUI dimensional).

**Verificação futura obrigatória:** confirmar visualmente que **todas** as texturas listadas aparecem corretamente, com 10×10 pixels onde aplicável, nítidas (amostragem nearest-neighbor/no duplo) e com o hover/estado escolhendo a variante certa — inclusive o alerta em seções indisponíveis e o X que dispensa o aviso verde.

---

## 6. Tabela de métodos/componentes que precisam ser alterados

> Somente itens que **realmente exigem** substituição ou adaptação. Componentes que podem permanecer sem alteração (§4) não aparecem aqui.

| Classe/arquivo NeoForge | Responsabilidade atual | Substituição/adaptação Forge | Risco de comportamento |
| --- | --- | --- | --- |
| `SimplestoragemanagerMod.java` — construtor | `@Mod`, config SERVER, registro no bus de mod e no bus de jogo | `FMLJavaModLoadingContext`, `MinecraftForge.EVENT_BUS`, `ModLoadingContext.registerConfig`, `ForgeConfigSpec` | Alto — mod não carrega |
| `SimplestoragemanagerMod.java` — `registerNetworking`, `MESSAGES`, `NetworkMessage`, `addNetworkMessage` | Catálogo e registro de 16 payloads via `RegisterPayloadHandlersEvent` | `NetworkRegistry.newSimpleChannel` + `registerMessage` com direção explícita; tipos POJO | Alto — nenhum pacote funciona |
| `SimplestoragemanagerMod.java` — `tick(ServerTickEvent.Post)` | Reavalia conexões dos menus abertos | `TickEvent.ServerTickEvent` | Médio |
| `config/SimplestoragemanagerConfig.java` | `protectConnectedInventories`, `scrollSpeed` | `ForgeConfigSpec` | Baixo |
| `init/SimplestoragemanagerModItems.java` — `REGISTRY` / `DeferredItem` | 4 itens | `DeferredRegister<Item>` + `RegistryObject` | Médio |
| `init/SimplestoragemanagerModItems.java` — `registerCapabilities` | Capability de 3 slots no item | `initCapabilities` + `LazyOptional<IItemHandler>` (ou evento, se disponível) | Alto — upgrades deixam de existir |
| `init/SimplestoragemanagerModMenus.java` — `STORAGE_MANAGER_GUI` | `MenuType` via `IMenuTypeExtension.create` | Construtor `MenuType` do Forge 1.20.1 + extra-data da mão | Alto |
| `init/SimplestoragemanagerModMenus.java` — `MenuAccessor.sendMenuStateUpdate` | Sincronização `PacketDistributor` | `SimpleChannel` | Médio |
| `init/SimplestoragemanagerModScreens.java` — `clientLoad` | `RegisterMenuScreensEvent` do NeoForge | Mesmo evento do Forge; imports/anotação | Baixo |
| `init/SimplestoragemanagerModRecipes.java` — `REGISTRY` | Serializer em `Registries.RECIPE_SERIALIZER` | `ForgeRegistries.RECIPE_SERIALIZERS` | Baixo |
| `init/SimplestoragemanagerModTabs.java` — `REGISTRY`, `buildTabContentsVanilla` | Aba criativa e `DeferredRegister` vazio | Evento do Forge; remover registro vazio | Baixo |
| `data/TerminalDataComponents.java` (3 componentes + registro) | `terminal_id`, `advanced`, `dimension_selection` | Helper NBT na ItemStack; remover `DeferredRegister.DataComponents` | Alto — identidade/modo/seleção perdidos |
| `item/StorageManagerItem.java` — `getName` | Nome basic/advanced vindo de componente | NBT boolean | Médio |
| `item/{Infinity,Dimensional,Multidimensional}InfinityUpgradeItem.java` — `appendHoverText` | Tooltip com Shift via `TooltipContext` | `Level`, `Screen.hasShiftDown()`, texto preservado | Médio |
| `item/UpgradeTooltip.java` — `appendDescription` | Quebra em 180 px com `getSplitter().splitLines` | Validar API; alternativa `font.split(Component, 180)` | Médio |
| `item/inventory/StorageManagerInventoryCapability.java` — classe inteira | `ComponentItemHandler` sobre `DataComponents.CONTAINER`, `ItemTossEvent` | Handler sobre NBT + `LazyOptional`; `ItemTossEvent` do Forge (bus FORGE) | Alto |
| `network/MenuStateUpdateMessage.java` | `CustomPacketPayload`, codecs, validação de tamanho | POJO + `encode/decode`, `CustomPayloadEvent.Context`, `SimpleChannel` | Alto |
| `network/DimensionalUpgradeOpenPayload.java` | Lista de dimensões + mão, S→C | `FriendlyByteBuf`, registro S→C | Alto |
| `network/DimensionalUpgradeSavePayload.java` | Seleção salva, C→S, escreve componente | `FriendlyByteBuf`, NBT no upgrade, `setChanged()` | Alto |
| `network/DimensionalUpgradeSaveResultPayload.java` | Resultado do salvamento, S→C | POJO + registro S→C | Médio |
| `network/TerminalViewPayload.java` — `STACK_CODEC`, `Section`, `VisibleSlot`, `Notice` | Snapshot remoto com quantidade/capacidade separadas | Codificação manual em `FriendlyByteBuf`; lógica de snapshot preservada | Alto — preview e contagens >64 quebram |
| `network/TerminalViewRequestPayload.java` | Pedido de busca/rolagem, C→S | `encode/decode` + registro C→S | Alto |
| `network/TerminalConnectionStatusPayload.java` | Atualização de status, S→C | Reutilizar codec de slot convertido; registro S→C | Alto |
| `network/TerminalFilterSavePayload.java` | Lista de ItemStack do filtro, C→S | `ItemStack.save(CompoundTag)` / `of(tag)` | Alto |
| `network/TerminalStorageClickPayload.java` | Cliques/arrastos + sequência, C→S | Codificação manual mantendo ordem dos campos | Alto — servidor perde autoridade |
| `network/TerminalShiftDragPreviewRequestPayload.java` | Pedido de prévia cumulativa, C→S | Codificação manual + registro C→S | Alto |
| `network/TerminalShiftDragPreviewPayload.java` | Prévia final, S→C | Codificação manual + registro S→C | Alto |
| `network/TerminalRenamePayload.java` | Renomear seção, C→S | `encode/decode` + registro C→S | Médio |
| `network/TerminalOrganizePayload.java` | Organizar seção, C→S | `encode/decode` + registro C→S | Médio |
| `network/TerminalLockTogglePayload.java` | Intenção de alternar cadeado, C→S | Mensagem vazia + registro C→S | Médio |
| `network/TerminalClearCraftingPayload.java` | Intenção de limpar grade, C→S | Mensagem vazia + registro C→S | Médio |
| `network/TerminalNoticeDismissPayload.java` | Dispensar aviso (String + `BlockPos`), C→S | `writeUtf(limite)`/`readUtf` + `writeBlockPos/readBlockPos` | Médio |
| `recipe/StorageManagerUpgradeRecipe.java` — `getResultItem`, `matches`, `assemble`, `isBasicTerminal` | Upgrade `isSpecial()` que copia o terminal | `CraftingInventory`, sem `HolderLookup`, NBT copiado no resultado | Alto — perda de filtros/cadeado/UUID |
| `recipe/StorageManagerUpgradeRecipe.StorageManagerUpgradeRecipeSerializer` | `MapCodec`, `streamCodec`, `streamCodec()` | `Codec<T>`, `fromNetwork/toNetwork(FriendlyByteBuf)` | Alto |
| `world/inventory/StorageManagerGUIMenu.java` — construtor/`readHand` | Slots + byte da mão vindo do `FriendlyByteBuf` | Conferir extra-data do `MenuType`/`MenuProvider` em 1.20.1 | Alto |
| `world/inventory/StorageManagerGUIMenu.java` — `terminalId`, `selectedDimensions`, `applyView` | Componentes do terminal | NBT na stack | Alto |
| `world/inventory/StorageManagerGUIMenu.java` — `playArmorEquipSound` | Som de equipar após confirmação | `Equipable`/`getEquipSound` 1.20.1 **[nome do método não confirmado]**; alternativa `ArmorItem` | Médio/Alto |
| `world/inventory/StorageManagerGUIMenu.java` — `slotsChanged`/`quickMoveCraftingResult` | Craft repetido com `CraftingInput`/`RecipeHolder` | `CraftingInventory`, `setRecipeUsed(ResourceLocation)`, sem `isItemEnabled` | Alto |
| `world/inventory/StorageManagerGUIMenu.java` — transferências (`isSameItemSameComponents`, `copyWithCount`, capability) | Inserção/extração/devolução em inventários remotos | `isSameItemSameTags`/`isSameItem`, `copy()+setCount()`, capability do Forge | Alto — muda o matching de itens |
| `world/inventory/PlayerArmorGuiSlot.java` — `canEquip`/`getMaxStackSize` | Validação de armadura, limite 1 | `canEquip` **[não confirmado]** ou `ArmorItem`; limite 1 preservado | Médio |
| `world/inventory/TerminalUpgradeSlot.java` | Regras de upgrade sobre a capability da stack | Handler NBT do Forge | Alto |
| `event/TerminalInteractionEvents.java` — `onRightClickBlock`, `onRightClickItem`, `onBreak` | Conectar/desconectar, proprietário, proteção, quebra | Bus FORGE, `CapabilityItemHandler`, `LazyOptional` | Alto |
| `event/TerminalInteractionEvents.java` — `openTerminal`, `openDimensionSelector`, `ensureTerminalId` | Abertura da GUI, lista de dimensões, UUID do terminal | `openMenu(...)` 1.20.1, `Registry.DIMENSION_REGISTRY`, NBT, `SimpleChannel` | Alto |
| `world/TerminalOriginMemory.java` — `onPlayerLoggedOut`, `onServerStopping` | Limpeza da memória de sessão | Imports/bus FORGE | Baixo |
| `world/TerminalSavedData.java` — `FACTORY`, `load`, `save` | Persistência de conexões/avisos/filtros | `SavedData.Factory` 2 args; `load/save(CompoundTag)` sem provider | Médio |
| `world/TerminalSavedData.java` — `ResourceLocation.parse`, `ResourceKey.create(Registries.DIMENSION, ...)` | Chaves de dimensão/bloco | `new ResourceLocation(...)` com validação, `Registry.DIMENSION_REGISTRY` | Baixo |
| `logic/InventoryFilterLogic.java` — `STACK_CODEC`, `toTag`, `fromTag` | Serialização do filtro | `save(CompoundTag)`, `ItemStack.of`, codificação manual | Médio |
| `logic/InventoryOrganizerLogic.java` — tags e `ResourceLocation.parse` | Classificação porNome/Quantidade/Tipo/Mod | `Registry.ITEM_REGISTRY`, `new ResourceLocation(...)` | Baixo/Médio |
| `access/InventoryAdapter.java` — `resolve`, `implementationKind`, `isChunkLoaded` | Disponibilidade sem carregar chunks | `CapabilityItemHandler` + `LazyOptional`; validar `hasChunk` **[não confirmado]** | Alto |
| `access/MultiblockDiscovery.java` | Descoberta por identidade de handler e por parceiro de baú | Capability do Forge; validar `ChestBlock.getConnectedDirection` | Alto |
| `integration/StorageDrawersManualClicks.java`, `StorageDrawersStableItemHandler.java` | Slots físicos estáveis, débito medido | Rebuild contra Storage Drawers Forge 1.20.1 | Alto |
| `integration/Sophisticated{Storage,Backpacks,Inventory}ManualClicks.java` | Preservação do slot clicado | Rebuild contra versões Forge 1.20.1 | Alto |
| `integration/JeiTerminalIntegration.java` | Transferência/hover/upgrade via JEI 19.x | API JEI 15.x Forge; assinaturas **[não confirmadas]** | Alto — risco do U listar todas as receitas |
| `integration/ReiTerminalIntegration.java` | Transferência/hover/`DefaultCustomShapedDisplay` | API REI 1.20.1; assinaturas **[não confirmadas]** | Alto — risco do U listar todas as receitas |
| `integration/EmiTerminalIntegration.java` | Bounds, handler e stack provider EMI 1.1+1.21.1 | API EMI Forge 1.20.1; assinaturas **[não confirmadas]** | Alto |
| `integration/RecipeViewerPriority.java` | Prioridade EMI>REI>JEI por reflexão | `ScreenEvent` do Forge + revalidação dos nomes internos de JEI/REI | Alto |
| `client/gui/StorageManagerGUIScreen.java` — `init` | Escolha da escala via `options.guiScale().get()` | `options.guiScale.get()` **[não confirmado]**; tabela SMALL/BIG preservada | Médio/Alto |
| `client/gui/StorageManagerGUIScreen.java` — `renderBg`/`drawRemoteSections`/`drawUnavailable`/`drawReplacementNotice`/`renderPlayer`/`renderTooltip` | Render manual, scissor, tooltips, `Graphics.blit` | Overloads `GuiGraphics` 1.20.1; `ClientTooltipPositioner` **[não confirmado]** | Médio |
| `client/gui/StorageManagerGUIScreen.java` — chamadas `PacketDistributor.sendToServer` | Ações do usuário | `SimpleChannel.sendToServer` | Alto |
| `client/gui/StorageManagerGUIScreen.java` — cores literais (§5.10) | `0x404040`, `0xE015120F`, `0xE01B4D27`, `0xFFfb8f13`, `0xFFFF5555` | Confirmar semântica ARGB/alpha na versão 1.20.1; ajustar conversão se necessário | Alto — textos invisíveis |
| `client/gui/DimensionalUpgradeSelectionScreen.java` — classes/métodos de texture e render | `ResourceLocation.fromNamespaceAndPath`, `tryParse`, `renderBackground`, som | `new ResourceLocation(...)` com validação, hook de render 1.20.1, som via `.value()` se aplicável | Médio |
| `src/main/resources/META-INF/neoforge.mods.toml` | Metadados NeoForge | `META-INF/mods.toml` (já preparado no alvo) | Alto |
| `src/main/resources/pack.mcmeta` | `pack_format: 48` | `pack_format: 15` (já preparado no alvo) | Alto — pack rejeitado |
| `data/simplestoragemanager/recipe/storage_manager_upgrade.json` | Ingrediente `minecraft:crafter` (1.21) | Substituir por item existente em 1.20.1 | Alto |
| `data/simplestoragemanager/recipe/*.json` (outros 4) | `"result": {"id","count"}` (1.20.5+) | `"result": "mod:item"` | Médio |
| `build.gradle`/`gradle.properties`/`settings.gradle` (originais) | Moddev 2.0.141, Java 21, NeoForge 21.1.262 | ForgeGradle 6, Java 17, Forge 47.4.28 (já preparados no alvo) | Alto |

---

## 7. Checklist para a futura portabilidade

> **Nenhum item abaixo está concluído.** Todos dependem da etapa futura de implementação.

### Fundação (P0)

- [ ] Definir `JAVA_HOME` para JDK 17 antes de qualquer comando Gradle.
- [ ] Executar `.\gradlew.bat genIntellijRuns` e remover o `.run/Minecraft Client (NeoForge 21.1.262).run.xml` residual.
- [ ] Executar `.\gradlew.bat dependencies --configuration compileClasspath` e registrar a resolução do Forge 1.20.1-47.4.28.
- [ ] Portar o bootstrap (`@Mod`, buses, config) e os registros `DeferredRegister` do Forge.
- [ ] Portar as três classes de configuração de metadados do mod para `mods.toml` (já preparado) e confirmar o carregamento em `runClient`.

### Dados do item (P0)

- [ ] Substituir os 3 DataComponents e `DataComponents.CONTAINER` por um helper NBT; atualizar **todos** os 10 pontos de leitura/escrita listados em §5.3.
- [ ] Decidir explicitamente se haverá migração de itens/mundos 1.21.1 → 1.20.1; se sim, implementar conversão de componentes para NBT.
- [ ] Validar que o UUID do terminal, o modo avançado, os 3 upgrades e a seleção de dimensões sobrevivem a salvar/recarregar.

### Rede (P0)

- [ ] Converter os 16 payloads para POJOs + `encode/decode` com `FriendlyByteBuf`.
- [ ] Registrar cada mensagem com direção explícita no `SimpleChannel`; conferir ordem dos campos e limites de string.
- [ ] Preservar as verificações de autoridade do servidor (`ServerPlayer`, `containerMenu`, limites).
- [ ] Testar em jogo (cliente + servidor): clique, arrasto, prévia de shift-arrasto, renomear, filtro, organizar, cadeado, busca, aviso verde, abertura da GUI dimensional.

### Recipe e crafting (P1)

- [ ] Converter o serializer e o `StorageManagerUpgradeRecipe` para a API 1.20.1.
- [ ] Trocar o ingrediente `minecraft:crafter` no JSON e o formato de `result` dos outros 4 recipes.
- [ ] Confirmar o loop de craft repetido e a devolução de sobras com a GUI aberta.

### Capabilities, menu e GUI (P1/P2)

- [ ] Implementar a capability de 3 slots no item via `initCapabilities`/`LazyOptional`.
- [ ] Adaptar menu/slots: índices 0–53 inalterados, `isSameItemSameComponents`→equivalente, `copyWithCount`, capability queries, som de equipar.
- [ ] Adaptar as telas: `GuiGraphics`, `ResourceLocation`, `guiScale`, key mappings, tooltips.
- [ ] **Verificar visualmente os textos** conforme §5.10 (alpha/ordem de canais; nenhum texto invisível).
- [ ] **Verificar visualmente todas as texturas de botão** conforme §5.12: 10×10 = imagem inteira reduzida, nunca recorte; atenção ao X de 8×8 vindo de PNG 14×14.

### Integrações (P2)

- [ ] Adicionar `compileOnly fg.deobf(...)` das versões 1.20.1 verificadas (§3.4) e compilar.
- [ ] Revalidar APIs de JEI 15.x, REI 1.20.1 e EMI 1.20.1 (nomes de métodos podem mudar).
- [ ] Revalidar os nomes internos usados por reflexão no `RecipeViewerPriority`.
- [ ] **Não registrar o Storage Manager como workstation/catalyst** em JEI, REI ou EMI.
- [ ] Validar manualmente Sophisticated Backpacks/Storage e Storage Drawers (clique manual, shift, arrasto, slots >64).

### Validação final

- [ ] `.\gradlew.bat build --no-daemon` com o código portado (incluindo `reobfJar`).
- [ ] `runClient` + `runServer`: matriz de clique/shift-clique/arrasto, cadeado trancado/destrancado, SMALL/BIG, singleplayer/multiplayer, vanilla 64 / mod 64 / slot >64.
- [ ] Confirmar persistência de `TerminalSavedData` (conexões, dono, filtros, organização, nomes customizados, prazo de uma hora para membros quebrados).
- [ ] Confirmar que nenhum chunk é carregado para verificar conexões.

---

## 8. Dúvidas, pressupostos e itens não confirmados

1. **Nenhuma classe do mod foi portada.** O projeto-alvo ainda não tem `src/main/java`; `compileJava` só terá significado após a etapa futura.
2. **ForgeGradle exato.** O MDK 47.4.28 usa a faixa `[6.0,6.2)`; nesta preparação ela resolveu para 6.0.54. Não foi fixada uma versão única.
3. **REI 1.20.1.** O artefato `12.1.786` existe no Maven Shedaniel, mas o metadata não declara explicitamente a versão do Minecraft/loader. Confirmar via POM/Modrinth antes de fixar.
4. **APIs de terceiros.** Os nomes exatos de métodos de JEI 15.x, REI 1.20.1, EMI 1.20.1, Sophisticated e Storage Drawers **não** puderam ser confirmados apenas com o código do projeto original (que compila contra as versões 1.21.1). As versões sugeridas em §3.4 vêm dos Maven oficiais, mas a compatibilidade com o código portado depende da compilação.
5. **Reflexão do `RecipeViewerPriority`.** Os nomes internos `mezz.jei.common.Internal`, `getClientToggleState`, `me.shedaniel.rei.api.client.config.ConfigObject`, `getHideKeybind` são específicos das versões 1.21.1 e podem não existir em 1.20.1 — a supressão de overlays pode falhar silenciosamente.
6. **EMI e a receita de upgrade.** O código atual não registra explicitamente a receita no EMI; presume-se que o plugin vanilla a exiba. Isso **não foi confirmado em jogo** nem para 1.20.1.
7. **Assinaturas 1.20.1 marcadas [VERIFICAR]:** `RegisterCapabilitiesEvent#registerItem`, `SavedData.Factory`, `Equipable`/`getEquipSound`, `canEquip`, `ServerChunkCache.hasChunk`, `ScreenEvent.KeyPressed.Pre` vs. `KeyPressed`, `ClientTooltipPositioner`, `renderEntityInInventoryFollowsMouse`, `options.guiScale`, `KeyMapping.isActiveAndMatches`, `ShapedRecipePattern` no serializer, `ResourceLocation.parse/tryParse`, `TagParser` (ausente no projeto).
8. **Migração de saves/itens.** Não há código de migração no repositório; não foi possível determinar a intenção dos autores a respeito de mundos criados em 1.21.1.
9. **Validação Gradle.** `gradlew --version`, `tasks --all`, `dependencies --configuration compileClasspath` e `build` terminaram com `BUILD SUCCESSFUL`, mas o `build` teve `compileJava NO-SOURCE`: ele valida apenas o setup, não a implementação do mod (§3.5).
10. **Filtro de amostragem das texturas** ao reduzir 20×20 → 10×10 pode diferir entre versões; confirmar nitidez na verificação visual.
11. **`accesstransformer.cfg` está vazio.** Não é possível concluir, apenas lendo o código, se serão necessários access transformers adicionais em 1.20.1 para APIs como `hasChunk` ou `ClientTooltipPositioner`.
12. **Cobertura de runtime.** A presença dos 16 handlers está confirmada no código, mas não é possível afirmar, sem executar o jogo, que todos são exercitados em uma sessão normal.
13. **Documentação Fabric anterior.** A menção a erros de transparência e ao registro indevido de workstation vem do histórico relatado pelo usuário e do `resumo.md`; o código Fabric em si **não** foi alterado nem usado como fonte de implementação nesta etapa.
