# Simple Storage Manager — preparação da portabilidade NeoForge 1.21.1 → Fabric 1.21.8

Este arquivo registra a auditoria, o setup Fabric e o resultado da migração de código.
O módulo NeoForge em `D:\Modding\SimpleStorageManager\NeoForge\1.21.1` não foi alterado.

## Versões e compatibilidade (validadas contra o `fabric-example-mod` oficial da branch 1.21.8)

| Item | Versão | Observação |
|---|---|---|
| Minecraft | `1.21.8` | alvo |
| Java (código e execução do jogo) | `21` | `options.release = 21`, `sourceCompatibility/targetCompatibility = 21` |
| Gradle (wrapper) | `9.8.2` | mesmo wrapper do exemplo oficial 1.21.8 |
| Fabric Loom | `1.17.21` | plugin `net.fabricmc.fabric-loom-remap`. Loom `1.19.x` **não** foi usado: exige JVM 25+ apenas para rodar o plugin, incompatível com o JDK 21 deste ambiente |
| Fabric Loader | `0.20.1` | |
| Fabric API | `0.136.1+1.21.8` | |
| Mappings | `loom.officialMojangMappings()` | proposital: o código NeoForge usa nomes Mojang (`net.minecraft.*`), o que permite portar a lógica comum quase literalmente |
| Storage Drawers (Fabric 1.21.8) | CurseForge `223852:6967708` (18.3.2) | `modCompileOnly`, não embutido; API `com.jaquadro.minecraft.storagedrawers.api.capabilities.IItemHandler` + `Capabilities.ITEM_HANDLER` |
| REI (Fabric 1.21.8) | CurseForge `310111:6833148` (20.0.811) | `modCompileOnly`, não embutido; plugin descoberto pelo entrypoint `rei_client` |
| Tipos de apoio da API REI | `me.shedaniel:RoughlyEnoughItems-api-fabric:20.0.811` (maven Shedaniel) | `modCompileOnly` adicional: o JAR do CurseMaven não traz POM transitivo; a API traz Architectury/cloth-config para `CompoundEventResult`, `FluidStack` e `Point` |

## Estrutura criada no projeto Fabric

```
Fabric/1.21.8/
  settings.gradle                 # repositório Fabric no pluginManagement; rootProject.name próprio
  build.gradle                    # Loom remap, splitEnvironmentSourceSets, deps, processResources
  gradle.properties               # versões acima + identidade do mod
  gradlew / gradlew.bat / gradle/wrapper/*
  .run/Minecraft Client (Fabric 1.21.8).run.xml   # configuração runClient
  .run/Minecraft Server (Fabric 1.21.8).run.xml   # configuração runServer
  src/main/java/.../SimpleStorageManager.java          # ModInitializer (common) — vazio
  src/client/java/.../client/SimpleStorageManagerClient.java # ClientModInitializer — vazio
  src/main/resources/fabric.mod.json
```

Para abrir no IntelliJ: **File → Open… → selecione `../build.gradle` (ou `../settings.gradle`) dentro
de `D:\Modding\SimpleStorageManager\Fabric\1.21.8`** e confirme "Open as Project".
O `rootProject.name` é `SimpleStorageManager-Fabric-1.21.8`, distinto do projeto NeoForge.

## Access widener / mixins

- Não existe **nenhum** mixin no projeto NeoForge (busca por `@Mixin`, `@Inject`, `@Shadow`,
  `mixins.json` retornou vazio).
- O `META-INF/accesstransformer.cfg` do NeoForge está **vazio** (apenas marcadores de bloco).
- Conclusão: **nenhum access widener e nenhum mixin são necessários nesta**.
  Caso precise substituir o evento `ItemTossEvent` (sem equivalente direto no Fabric,
  confirmado: o Fabric API não expõe callback de drop de item), aí sim um mixin pequeno e
  localizado em `LivingEntity.drop(...)`/criação de `ItemEntity` poderá ser justificado e
  documentado aqui — apenas para fechar o menu quando o terminal é jogado fora.

## Pontos de entrada dependentes do loader

### Bootstrap / registro / eventos / config
- `SimplestoragemanagerMod.java`: `@Mod`, `IEventBus`, `NeoForge.EVENT_BUS.register(this)`,
  `registerConfig(ModConfig.Type.SERVER, ...)`, `RegisterPayloadHandlersEvent` +
  `PayloadRegistrar.playBidirectional`, `SidedThreadGroups.SERVER`, `ServerTickEvent.Post`.
- `init/SimplestoragemanagerModItems.java`: `DeferredRegister.Items` com 4 itens;
  `RegisterCapabilitiesEvent.registerItem(...)` para o handler de upgrades.
- `init/SimplestoragemanagerModTabs.java`: `BuildCreativeModeTabContentsEvent` injeta os 4 itens
  em `CreativeModeTabs.FUNCTIONAL_BLOCKS` (nenhum tab novo é registrado).
- `init/SimplestoragemanagerModMenus.java`: `DeferredRegister<MenuType<?>>` com
  `IMenuTypeExtension.create(StorageManagerGUIMenu::new)`; helper `MenuAccessor` usa
  `PacketDistributor` com `player.level().isClientSide`.
- `init/SimplestoragemanagerModScreens.java`: `@EventBusSubscriber(Dist.CLIENT)` +
  `RegisterMenuScreensEvent` (client-only).
- `init/SimplestoragemanagerModRecipes.java`: `DeferredRegister<RecipeSerializer<?>>` com
  `storage_manager_upgrade`; **tipo de receita é vanilla `RecipeType.CRAFTING`**.
- `data/TerminalDataComponents.java`: `DeferredRegister.DataComponents` com `terminal_id` (UUID),
  `advanced` (boolean) e `dimension_selection` (List<String>).
- `config/SimplestoragemanagerConfig.java`: `ModConfigSpec` SERVER com
  `protectConnectedInventories` (default false) e `scrollSpeed` (27, 1–500).
  Atenção: `scrollSpeed` é lido no **cliente** (`StorageManagerGUIScreen.mouseScrolled`) embora
  esteja no spec SERVER — a sincronização automática do NeoForge não existe no Fabric.
- `event/TerminalInteractionEvents.java`: `PlayerInteractEvent.RightClickBlock`,
  `RightClickItem`, `BlockEvent.BreakEvent` com `EventPriority.LOWEST`,
  `Capabilities.ItemHandler.BLOCK` e `PacketDistributor`.
- `world/TerminalOriginMemory.java`: `PlayerEvent.PlayerLoggedOutEvent`, `ServerStoppingEvent`.
- `item/inventory/StorageManagerInventoryCapability.java`: `ComponentItemHandler`,
  `MutableDataComponentHolder`, `ItemTossEvent` (fecha o menu quando o terminal é drogado).

### Capacidades, inventários e persistência
- `access/InventoryAdapter.java` + `access/MultiblockDiscovery.java`: `IItemHandler`,
  `InvWrapper`, `SidedInvWrapper`, `Capabilities.ItemHandler.BLOCK`, `ModList.isLoaded`,
  `ServerChunkCache.hasChunk` (nunca carrega chunks). Agrupamento de multibloco por identidade
  de referência do handler; baús pelo parceiro do estado.
- `world/TerminalSavedData.java`: `SavedData` + `Factory`, NBT `CompoundTag`, overworld como
  storage. Persiste conexões, dono, cadeado, nomes customizados, filtros, config de organização,
  avisos de substituição e IDs de bloco por membro — tudo por UUID de terminal.
- `StorageManagerGUIMenu.java`, `logic/InventoryOrganizerLogic.java`,
  `integration/StorageDrawers*`/`Sophisticated*`: leitura/escrita via `IItemHandler`.
- Os 3 upgrades ficam no componente vanilla `minecraft:container` do ItemStack do terminal.
  O NeoForge usa `ComponentItemHandler` para isso; no Fabric será preciso implementar um
  `Storage<ItemVariant>` próprio sobre `DataComponentTypes.CONTAINER` — não existe classe
  equivalente. Internamente o mod já instancia o handler diretamente no ItemStack; o registro
  de capability serve apenas para interoperabilidade com outros mods.
- Agrupamento de multibloco usa identidade de referência (`==`) do handler em
  `MultiblockDiscovery.isSameMultiblockInventory`; no Fabric isso deve ser redesenhado para
  comparar a entidade de bloco, pois storages podem não ser estáveis entre consultas.
- `TerminalSavedData` é global (sempre salvo no storage da dimensão overworld) e usa NBT
  `CompoundTag`. No Fabric, `PersistentState` é por mundo: buscar explicitamente o
  `PersistentStateManager` do overworld para preservar o formato de save e a semântica global.
- `TerminalOriginMemory` é memória de sessão (estático), sem NBT — limpa no logout e no
  servidor parar. Não deve ser persistido no port.

### Rede (16 payloads)
- Todos usam `CustomPacketPayload` + `StreamCodec` + `IPayloadContext` e são registrados
  centralmente com `playBidirectional`. Na prática: **10 C2S**, **5 S2C**, apenas
  `MenuStateUpdateMessage` é genuineamente bidirecional.
- Três cópias do mesmo codec custom de `ItemStack` (count separado + NBT) precisam ser
  preservadas: `TerminalViewPayload`, `TerminalShiftDragPreviewPayload`,
  `InventoryFilterLogic.STACK_CODEC` — é isso que mantém contagens acima de 99.
- Pares requisição/resposta: `TerminalViewRequest→TerminalView`,
  `ShiftDragPreviewRequest→ShiftDragPreviewResult`,
  `DimensionalUpgradeSave→DimensionalUpgradeSaveResult`,
  `TerminalStorageClick→TerminalView(operationSequence)`.

### Cliente e integrações opcionais
- `client/gui/*`: `Minecraft.getInstance().options.guiScale().get()` para escolher SMALL/BIG,
  `PacketDistributor.sendToServer`, tooltips/scissor manuais, `EditBox`, key mapping de agachar.
- `init/SimplestoragemanagerModScreens.java` e `integration/RecipeViewerPriority.java` são
  client-only (`Dist.CLIENT`, `ScreenEvent.Opening/Closing/KeyPressed.Pre`, `ModList.isLoaded`,
  reflexão para EMI/REI/JEI).
- Integrações `modCompileOnly`: Storage Drawers e REI, com JARs Fabric 1.21.8 confirmados e
  **nunca** embutidos no artefato final. EMI e JEI não têm integração neste build; o
  `RecipeViewerPriority` apenas detecta seus ids e usa reflexão protegida, sem dependência
  obrigatória.

## Tabela de migração (auditoria)

| Classe/arquivo NeoForge | Responsabilidade atual | Substituição/adaptação Fabric                                                                                                                                                                                                                                                         | Risco de comportamento |
|---|---|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---|
| `../build.gradle`, `../gradle.properties`, `../settings.gradle` | moddev 2.0.141 + NeoForge 21.1.262, repos Modrinth/Shedaniel/Jared, deps `compileOnly` | **Já feito**: Loom `net.fabricmc.fabric-loom-remap` 1.17.21, repositório Fabric, Java 21, Mojang mappings. Adicionar apenas `modCompileOnly` das variantes Fabric dos mods opcionais                                                                                                  | Baixo (setup validado em configuração) |
| `META-INF/neoforge.mods.toml` | id/versão, range NeoForge, deps EMI/REI opcionais | `fabric.mod.json` (já criado) com `"main"` + `"client"`, `depends` `fabricloader>=0.20.1`, `minecraft ~1.21.8`, `java>=21`, `fabric-api>=0.136.1`. Declarar EMI/JEI/REI como opcionais (`"suggests"`)                                                                                 | Médio — faixas de versão e nomes de mod id Fabric precisam ser conferidos |
| `META-INF/accesstransformer.cfg` (vazio) | nenhuma regra real | Removido. Nenhum access widener necessário                                                                                                                                                                                                                                            | Baixo |
| `pack.mcmeta` | metadados de resource/data pack do NeoForge | Não incluir no JAR Fabric; `assets/` e `data/` ficam na raiz do resources                                                                                                                                                                                                             | Baixo |
| `SimplestoragemanagerMod.java` | `@Mod`, `IEventBus`, `registerConfig`, `NeoForge.EVENT_BUS`, `PayloadRegistrar`, `SidedThreadGroups.SERVER`, `ServerTickEvent.Post`, `queueServerWork` | `SimpleStorageManager implements ModInitializer` (já criado, vazio). Registro direto no `onInitialize`; `ServerTickEvents.END_SERVER_TICK`; checagem de thread de servidor própria; callbacks de interação/logout/break                                                               | **Alto** — a checagem `SidedThreadGroups.SERVER` não existe; `queueServerWork` pode descartar trabalho enfileirado se trocado incorretamente |
| `init/SimplestoragemanagerModItems.java` | 4 itens via `DeferredRegister.Items`; `RegisterCapabilitiesEvent` para upgrades | `Registry.register(Registries.ITEM, new Identifier(MODID, ...), ...)` para `storage_manager`, `infinity_upgrade`, `dimensional_infinity_upgrade`, `multidimensional_infinity_upgrade`. Upgrades: `Storage<ItemVariant>` próprio sobre `DataComponentTypes.CONTAINER` (sem `ComponentItemHandler`) | **Alto** — Fabric Transfer API não tem equivalente a `IItemHandler`/`ComponentItemHandler`; não existe `ItemStorage.ITEM`, use `ItemApiLookup` |
| `init/SimplestoragemanagerModTabs.java` | injeta 4 itens em `FUNCTIONAL_BLOCKS` | `ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(...)`                                                                                                                                                                                                             | Baixo |
| `init/SimplestoragemanagerModMenus.java` | `DeferredRegister<MenuType<?>>` + `IMenuTypeExtension.create`; `MenuAccessor` (usado por `MenuStateUpdateMessage`) | `Registry.register(Registries.MENU, ...)` com `new MenuType<>(StorageManagerGUIMenu::new)`. Separar o ramo cliente do `MenuAccessor` (ele importa `Minecraft` num arquivo comum — risco de carregar classe client em servidor dedicado)                                               | Médio — verificar construtor `MenuType` e semântica de FeatureFlags no 1.21.8 |
| `init/SimplestoragemanagerModScreens.java` | `RegisterMenuScreensEvent` (`Dist.CLIENT`) | `HandledScreens.register(...)` no `ClientModInitializer` (já criado, vazio)                                                                                                                                                                                                           | Baixo |
| `init/SimplestoragemanagerModRecipes.java` | `DeferredRegister<RecipeSerializer<?>>` | `Registry.register(Registries.RECIPE_SERIALIZER, ...)`. `RecipeType` continua vanilla `CRAFTING`                                                                                                                                                                                      | Baixo |
| `data/TerminalDataComponents.java` | 3 componentes: `terminal_id` (UUID), `advanced` (bool), `dimension_selection` (List<String>) | `DataComponentType.builder()...build()` + `Registry.register(Registries.DATA_COMPONENT_TYPE, ...)`, mesmos codecs/stream codecs                                                                                                                                                       | Médio — conferir nomes do builder e `ByteBufCodecs` com Mojang mappings 1.21.8 |
| `config/SimplestoragemanagerConfig.java` | `ModConfigSpec` SERVER: `protectConnectedInventories`, `scrollSpeed` | Sem equivalente nativo: Cloth Config/owo-lib ou loader próprio. **`scrollSpeed` é lido no cliente**, então é obrigatório sincronizar manualmente (payload ou sincronização do config)                                                                                                 | **Alto** — sem sincronização a rolagem da GUI fica sempre no default |
| `event/TerminalInteractionEvents.java` | `RightClickBlock`, `RightClickItem`, `BlockEvent.BreakEvent` (LOWEST), capability probe, `PacketDistributor` | `UseBlockCallback.EVENT`, `UseItemCallback.EVENT`, `PlayerBlockBreakEvents.BEFORE` + `ItemStorage.SIDED.find`; envio por `ServerPlayNetworking`                                                                                                                                       | **Alto** — semântica de cancelamento/InteractionResult diferente; ordem de prioridade `LOWEST` não tem análogo direto |
| `world/TerminalOriginMemory.java` | memória FIFO de sessão; `PlayerLoggedOutEvent`, `ServerStoppingEvent` | `ServerPlayConnectionEvents.DISCONNECT`, `ServerLifecycleEvents.SERVER_STOPPING`; manter como estado estático sem NBT                                                                                                                                                                 | Baixo-Médio |
| `item/inventory/StorageManagerInventoryCapability.java` | `ComponentItemHandler` (3 slots em `minecraft:container`), `ItemTossEvent` fecha menu | Implementar `Storage<ItemVariant>` sobre `ItemContainerContents` (API pública); para fechar menu no drop, **mixin pequeno** em `LivingEntity.drop(...)` (não há callback Fabric para item toss)                                                                                       | **Alto** — único ponto que justifica mixin; documentar o motivo |
| `SimplestoragemanagerMod` (bootstrap de rede) | `MESSAGES` + `playBidirectional` para todos os 16 payloads | `PayloadTypeRegistry.playC2S()/playS2C()` + receivers. **Somente `MenuStateUpdateMessage` é realmente bidirecional**; 10 C2S e 5 S2C devem ser separados                                                                                                                              | **Alto** — o registro atual "funciona" por causa dos guards de flow/player, mas é semanticamente errado |
| `network/*` (16 payloads) | `CustomPacketPayload` + `StreamCodec` + `IPayloadContext`; 3 cópias do codec de ItemStack com contagem fora do NBT | Mesmos records e codecs (Mojang mappings ajudam muito), trocando contexto por `PacketContext`, `enqueueWork` por execução no player/server, e removendo `@EventBusSubscriber`/`FMLCommonSetupEvent`                                                                                   | **Alto** — preservar os 3 codecs de ItemStack (contagens >99), `ComponentSerialization`, `BlockPos`, UUID e a validação de tamanho do `MenuStateUpdateMessage` |
| Envios em `StorageManagerGUIMenu`, `StorageManagerGUIScreen`, `DimensionalUpgradeSelectionScreen`, `DimensionalUpgradeSavePayload`, `TerminalInteractionEvents`, `ModMenus` | `PacketDistributor`/`player.connection.send` | `ServerPlayNetworking.send(player, payload)` / `ClientPlayNetworking.send(payload)`                                                                                                                                                                                                   | Médio-Alto — cada ponto precisa da direção correta |
| `access/InventoryAdapter.java` | `IItemHandler`, `Capabilities.ItemHandler.BLOCK`, `InvWrapper`/`SidedInvWrapper`, `hasChunk` | `ItemStorage.SIDED.find(world, pos, side)` → `Storage<ItemVariant>`; `StorageUtil.insert/extract` com `Transaction`; `ServerChunkManager.hasChunk`                                                                                                                                    | **Muito alto** — reescrita de toda a camada de acesso a inventário |
| `access/MultiblockDiscovery.java` | agrupa por identidade `==` do `IItemHandler`, baús pelo parceiro, limite 5×5×5 | Mesma BFS com `ItemStorage.SIDED.find`, mas comparar a **entidade de bloco**, não a referência do storage (storages podem não ser estáveis)                                                                                                                                           | **Muito alto** — a regra de agrupamento muda; risco de conexões antigas serem separadas ou agrupadas errado |
| `world/TerminalSavedData.java` | `SavedData` global no overworld, `CompoundTag`, `setDirty` | `PersistentState` + `getPersistentStateManager().getOrCreate(...)` do **overworld explicitamente**, `markDirty()`, mesmo layout NBT                                                                                                                                                   | Médio — `PersistentState` é por mundo; manter chave `simple_storage_manager_terminals` e o NBT idêntico para compatibilidade de save |
| `logic/*` (`InventoryOrganizerLogic`, `TerminalInsertionRules`, `ManualInsertionRules`, `ShiftDragPlanner` etc.) | regras puras, mas `IItemHandler` em algumas classes | Manter as regras; trocar assinaturas para `Storage<ItemVariant>`/`SingleSlotStorage`; `TerminalScreenLayout` é dado puro e pode ser compartilhado                                                                                                                                     | Médio-Alto — as regras em si são a referência de comportamento e não devem ser alteradas |
| `client/gui/StorageManagerGUIScreen.java` | `AbstractContainerScreen`, scissor/tooltips manuais, `EditBox`, `guiScale`, key mappings, `PacketDistributor` | Praticamente 1:1 com Mojang mappings (`AbstractContainerScreen`, `GuiGraphics`, `EditBox`, `options.guiScale()`), só trocando os envios de rede e o registro da tela                                                                                                                  | Médio — hit-test/tooltips e a tabela SMALL/BIG devem ser regression-testados |
| `client/gui/DimensionalUpgradeSelectionScreen.java` | `Screen` vanilla + `PacketDistributor.sendToServer` | `Screen` vanilla + `ClientPlayNetworking.send`                                                                                                                                                                                                                                        | Baixo |
| `client/gui/TerminalScreenLayout.java` | constantes de layout (puro) | Manter como código compartilhado (mover de pacote `client.gui` se desejar)                                                                                                                                                                                                            | Baixo |
| `world/inventory/*` (menu e slots) | `AbstractContainerMenu`, `ResultSlot`, crafting, upgrades | Mesmas classes com Mojang mappings; MenuType registrado no entrypoint comum; cuidado com o acoplamento a `TerminalScreenLayout`                                                                                                                                                       | Médio — `IMenuTypeExtension.create` não existe |
| `item/UpgradeTooltip.java` e upgrades | tooltip com `Minecraft.getInstance()`/key mapping | `@Environment(EnvType.CLIENT)` ou arquivo no source set `client`                                                                                                                                                                                                                      | Baixo |
| `integration/EmiTerminalIntegration.java` | `@EmiEntrypoint`, `EmiPlugin` | Não portado: EMI não é suportado neste build                                                                                                                                                                                                                                              | Baixo |
| `integration/JeiTerminalIntegration.java` | `@JeiPlugin`, `IModPlugin` | Removido; substituído pelo plugin REI                                                                                                                                                                                                                                                     | Baixo |
| `integration/ReiTerminalIntegration.java` | `@REIPluginClient` do pacote **Forge/NeoForge** | Sem anotação: `REIClientPlugin` descoberto pelo entrypoint `rei_client`. `FocusedStackProvider.provide` usa `dev.architectury.event.CompoundEventResult<EntryStack<?>>` (API real do REI 20.0.811)                                                                                       | Médio — validar shortcuts com REI instalado |
| `integration/RecipeViewerPriority.java` | `ScreenEvent.Opening/Closing/KeyPressed.Pre` com `EventPriority.HIGHEST`, reflexão EMI/REI/JEI | Fabric `ScreenEvents`/`ScreenKeyboardEvents` + polling de `client.screen`, `FabricLoader.isModLoaded`; REI suprimido via `ConfigObject` por reflexão protegida                                                                                                                      | **Alto** — manter a regra EMI > REI > JEI e não sobrescrever estado já desligado pelo jogador |
| `integration/StorageDrawers*` | APIs NeoForge (`PlatformDrawerItemHandler`, `Capabilities.ItemHandler.BLOCK`) | Adaptado: `DrawerItemHandler`, `Capabilities.ITEM_HANDLER.getCapability(level, pos)` e `InventoryAccess` no handler estável; mapeamento `FIRST_HANDLER_SLOT = 1` preservado                                                                                                         | Médio — validar cliques manuais com Storage Drawers instalado |
| `integration/Sophisticated*` | `InventoryHandler.insertItemOnlyToSlot`, `getExternalItemHandler` | Não portado: sem equivalente Fabric 1.21.8; caminho genérico via `InventoryAccess`                                                                                                                                                                                                       | Baixo |
| `assets/`, `data/` | texturas, lang, models, recipes | Copiar para `../src/main/resources`, sem `pack.mcmeta`. Manter os IDs `simplestoragemanager:*`                                                                                                                                                                                 | Baixo |
| `elements/`, `simplestoragemanager.mcreator`, `mcreator.gradle`, `.run/...NeoForge...` | metadados MCreator e config de run do NeoForge | Não copiar. O `.run` do NeoForge foi removido e substituído por configs Fabric                                                                                                                                                                                                        | Baixo |

## Próximos passos — (não executados aqui)

1. Separar o código comum (lógica, menu, dados, payloads, acesso) do código client (telas,
   HandledScreens, integrações de recipe viewers) nos source sets `main`/`client`.
2. Substituir registro NeoForge por `Registry.register`, eventos por callbacks Fabric API,
   capabilities por Fabric Transfer API e `SavedData` por `PersistentState`.
3. Reescrever o registro de rede com `PayloadTypeRegistry` + `ServerPlayNetworking` /
   `ClientPlayNetworking`, separando as direções corretamente.
4. Escolher/implementar config com sincronização manual de `scrollSpeed`.
5. Copiar `assets/` e `data/` (texturas, lang, models, recipes) — exceto `pack.mcmeta`,
   que não é necessário no JAR Fabric — e validar em `runClient`/`runServer`.


## Resultado da migração

### Substituições de API
- `@Mod`/`IEventBus`/`DeferredRegister` → `ModInitializer` + `Registry.register` com `BuiltInRegistries`.
- `Capabilities.ItemHandler.BLOCK`/`IItemHandler` → `ItemStorage.SIDED.find` + `Storage<ItemVariant>` atrás de `InventoryAccess` (consulta, simulação e execução por slot, com transações).
- `ComponentItemHandler` → `ItemContainerContents` + `InventoryStorage.of` para os 3 slots de upgrade.
- `SavedData.Factory` → `SavedDataType` com `Codec` sobre `CompoundTag`, mantendo o nome `simple_storage_manager_terminals` no overworld.
- `ModConfigSpec` → JSON próprio em `config/simplestoragemanager-server.json`, com `scrollSpeed` sincronizado ao cliente via payload.
- `PacketDistributor`/`IPayloadContext` → `PayloadTypeRegistry`, `ServerPlayNetworking`, `ClientPlayNetworking`; 11 C2S e 6 S2C (sendo `MenuStateUpdateMessage` bidirecional).
- `RegisterMenuScreensEvent` → `MenuScreens.register` com access widener (a Fabric API 1.21.8 removeu `HandledScreens`).
- Eventos NeoForge → `UseBlockCallback`, `UseItemCallback`, `PlayerBlockBreakEvents.BEFORE`, `ServerTickEvents`, `ServerPlayConnectionEvents`, `ServerLifecycleEvents`, `ItemGroupEvents`.
- `ItemTossEvent` → mixin local em `LivingEntity.drop` (não existe callback Fabric equivalente).
- `ModList.isLoaded` → `FabricLoader.getInstance().isModLoaded`.

### Integrações opcionais
- REI 20.0.811: `modCompileOnly` de `curse.maven:roughly-enough-items-310111:6833148`; plugin client descoberto pelo entrypoint `rei_client`. Terminal como workstation de crafting, transfer handler para os slots `CRAFTING_GRID_START`/`CRAFTING_GRID_COUNT` e focused-stack provider com prioridade 100. A supressão do overlay em `RecipeViewerPriority` usa `ConfigObject` por reflexão protegida, sem dependência obrigatória.
- Storage Drawers 18.3.2: `modCompileOnly` de `curse.maven:storage-drawers-223852:6967708`. `IItemHandler` do mod e `Capabilities.ITEM_HANDLER.getCapability(level, pos)` substituem os tipos NeoForge; `StorageDrawersStableItemHandler` implementa `InventoryAccess` e preserva o mapeamento de slots (`FIRST_HANDLER_SLOT = 1`, ordem de `getAccessibleDrawerSlots`), `canStore`, capacidade e inserts/extracts simulados. Os ramos de clique manual do menu foram reativados.
- EMI e JEI: não suportados neste build; `RecipeViewerPriority` trata ausência de classes como no-op seguro e mantém a preferência EMI > REI > JEI quando presentes.
- Sophisticated Storage/Backpacks: não portados (APIs NeoForge sem equivalente Fabric 1.21.8); inventários conectados seguem pelo caminho genérico `InventoryAccess`/Transfer API.

### Diferenças em relação ao NeoForge
- Config em JSON em vez de TOML; `scrollSpeed` é sincronizado explicitamente ao entrar no mundo.
- Agrupamento de multiblocos usa identidade da entidade de bloco (storages Fabric não são garantidamente estáveis entre consultas).
- `fabric.mod.json` declara `suggests` apenas para `roughlyenoughitems` e `storagedrawers`, com entrypoint `rei_client`.