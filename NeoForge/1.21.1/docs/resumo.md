# Simple Storage Manager - documentacao do Mod
Versão do Java: 21

## Arquitetura atual

- `StorageManagerGUIMenu` mantém os slots do jogador, armaduras, mão secundária, upgrades e a grade temporária 3x3 com resultado vanilla. Inventários conectados continuam fora dos slots do menu.
- Inventários conectados nunca são representados por `Slot` do menu. O servidor os sincroniza em snapshots próprios; o snapshot contém todos os slots conectados, inclusive os que estão fora da área visível, para que a previsão use o mesmo estado-base da operação.
- Cliques e arrastos em inventários conectados usam `TerminalStorageClickPayload`; operações em lote carregam um número de sequência e o cliente mantém a previsão até receber um único `TerminalViewPayload` de confirmação com os slots remotos e locais finais. Snapshots com sequência anterior são ignorados. Arrastos enviam a lista ordenada de alvos ao soltar, sem pacote durante o movimento.
- Inserções remotas comuns simulam `insertItem` no slot de destino. Cliques e arrastos manuais sem Shift usam integrações específicas para inventários Sophisticated e Storage Drawers, preservando o slot clicado; shift-clique e shift-arrasto continuam no caminho comum e percorrem pilhas compatíveis antes de slots vazios.
- A inserção por shift-click (com e sem arrasto) a partir do inventário do player segue a regra de slot válido por inventário: a procura percorre os inventários conectados na ordem, tentando primeiro pilhas parciais e depois slots vazios; quando o slot válido encontrado pertence a um inventário cujo filtro impede a inserção, a procura continua no próximo inventário conectado. A pilha só volta ao inventário do player quando nenhum slot válido e aceito existe em nenhum inventário conectado.
- As extrações usam o limite do item, independentemente do limite do slot: clique esquerdo e shift-clique retiram no máximo uma pilha do item; clique direito retira `min(ceil(contagemNoSlot / 2), max(1, limiteItem / 2))`, com a contagem lida no servidor no momento do clique. O shift-click simula a extração e o espaço em principal/hotbar antes de alterar a origem; se não houver destino, nada é extraído.
- O botão de cadeado usa `TerminalLockTogglePayload`, que envia somente a intenção de alternar. O estado é decidido no servidor, validado por `TerminalOwnershipRules` e sincronizado no snapshot da GUI; não proprietários ainda podem ver o estado e a tooltip.
- Com o cadeado ativado, cada extração de inventário conectado registra em memória de sessão o jogador, terminal, item e componentes, conexão, slot, quantidade e ordem FIFO. Uma inserção correspondente tenta devolver primeiro ao inventário de origem, resolvendo-o novamente e revalidando carregamento, alcance, dimensão e bloco; dentro dele, completa pilhas compatíveis antes de ocupar slots vazios. Em clique manual na própria origem, o destino é o slot clicado; ao clicar em outra conexão, a origem tem prioridade. Clique direito em outra conexão devolve exatamente uma unidade do cursor à origem. Se a origem estiver indisponível ou sem espaço, o excedente segue a regra padrão. A memória não mantém handlers em cache, não é salva em NBT e é descartada ao destrancar, ao jogador sair e ao servidor parar.
- O estado do cadeado fica no `TerminalSavedData` persistente por UUID do terminal. Ao reabrir, `sendView` envia o valor atual junto do snapshot; a GUI escolhe a textura a partir desse valor e do hover atual. O estado padrão de terminais antigos ou sem campo salvo é destrancado.
- Shift+clique e Shift+clique+arrasto esquerdo em slots do jogador enviam a intenção ao servidor; a quantidade e todos os destinos são decididos lá. No cadeado trancado, a origem revalidada tem prioridade e o excedente segue a inserção normal. No shift-arrasto, cada slot de origem é processado uma vez, na ordem visitada; pilhas parciais recebem itens antes de slots vazios, repetindo a busca enquanto houver excedente. O servidor confirma de uma vez os slots locais e remotos finais. Com o cadeado trancado e sem registro de origem, o shift-arrasto soma as pilhas no mesmo slot de destino até atingir o limite do item ou do slot antes de utilizar outro slot válido.
- Ao equipar uma peça nos slots de armadura da GUI, o menu toca o som definido pelo item somente após confirmar a alteração no servidor.
- Quando o snapshot não contém conexões nem avisos, a área principal mostra uma instrução centralizada para conectar o primeiro inventário.
- Shift+clique de extração simula no servidor a extração e a inserção em pilhas parciais e slots vazios do inventário principal, depois na hotbar. Só então executa ambas; qualquer excedente inesperado retorna ao slot de origem.
- Seções indisponíveis recebem fundo escuro e faixas diagonais laranjas, com uma área escura atrás do aviso central. O recorte do conteúdo continua aplicado pelo scissor principal.
- A tela desenha tooltips dos slots manualmente. A tooltip de item remoto só aparece sobre um slot remoto válido dentro da área principal da variante atual; a hit-test dos slots locais também usa suas posições customizadas, incluindo a variante BIG, sem suprimir tooltips de slots do jogador, upgrades ou crafting.
- A pesquisa usa `TerminalViewRequestPayload` e é executada no servidor sobre todos os slots de todas as conexões.
- O ItemStack guarda apenas `terminal_id`. Conexões, membros de multiblocos, membros quebrados, avisos, proprietário e estado persistente do cadeado ficam em `TerminalSavedData` no save do mundo.
- `TerminalSavedData` também persiste o UUID do proprietário do terminal. A propriedade é definida na primeira conexão de inventário; outros jogadores podem consultar a GUI, mas operações pelo terminal e alterações de conexões são recusadas no servidor.
- O arquivo de configuração SERVER `simplestoragemanager-server.toml` define `protectConnectedInventories` (padrão `false`). Quando ativo, impede conectar um inventário atualmente vinculado a outro terminal.
- A grade e o resultado de crafting existem no menu e são sincronizados pelo sistema normal de slots. O servidor consulta as receitas `CRAFTING` e usa `ResultSlot` para consumir ingredientes e devolver sobras de receitas especiais.
- Shift-clique simples nos slots de upgrade, armadura, mão secundária e crafting tenta mover a pilha inteira primeiro para um slot compatível do inventário principal, depois para a hotbar e, por fim, para o primeiro slot vazio disponível dos inventários conectados. Se nenhum destino comportar a pilha inteira, a operação não a move. O shift-clique e o shift-arrasto no resultado repetem a receita enquanto o resultado e a receita continuarem iguais e houver destino para cada produto.
- Shift-clique + arrasto iniciado em upgrades, armadura ou crafting extrai progressivamente para pilhas compatíveis e slots vazios do inventário principal/hotbar, sem usar inventários conectados como destino. A prévia de upgrades é local e imediata; upgrades inferiores só são previstos e removidos quando os superiores já foram totalmente transferidos na sequência visitada. Ao soltar, o servidor replaneja e executa a operação.
- O payload `TerminalClearCraftingPayload` envia somente a intenção do botão X; toda inserção e decisão sobre itens ocorre no servidor.
- Ao limpar a grade ou fechar o menu, cada pilha tenta primeiro completar pilhas e ocupar slots vazios do inventário principal, depois a hotbar e então os primeiros slots vazios dos inventários conectados, na ordem salva. O botão mantém na grade qualquer sobra sem destino; ao fechar, a sobra é derrubada perto do jogador.
- Nenhum chunk é carregado para inserir itens em inventários conectados: conexões indisponíveis ou descarregadas são ignoradas.
- No modo básico, cada conexão só fica disponível na mesma dimensão, dentro de 500 blocos do centro do bloco conectado e com todos os chunks de seus membros carregados. A conexão permanece salva enquanto indisponível. Com qualquer upgrade instalado, não se aplica o limite de distância; a validação de chunks continua ativa.
- Os upgrades são armazenados nos três slots do componente `minecraft:container` do próprio terminal: nível 3 no slot superior, nível 2 no médio e nível 1 no inferior. A colocação respeita a posição e a dependência dos níveis; níveis inferiores não podem ser removidos enquanto houver um nível superior instalado. Shift-clique do inventário do jogador não transfere upgrades para esses slots. Shift-clique em um upgrade instalado tenta movê-lo, nesta ordem, para um slot compatível do inventário principal, hotbar e inventários conectados disponíveis; se não encontrar destino, não o remove.
- O nível efetivo é o upgrade de maior nível funcional: nível 2 vazio (sem dimensões selecionadas) não conta e deixa o nível 1 ativo; nível 2 com dimensões selecionadas permite qualquer distância apenas nelas; nível 3 permite qualquer distância e dimensão. Em todos os níveis, os chunks dos inventários conectados precisam estar carregados.
- As dimensões do nível 2 são selecionadas na GUI aberta ao usar o item `dimensional_infinity_upgrade`. A tela lista as dimensões registradas no servidor, permite selecionar até cinco e envia a seleção somente ao salvar. O servidor valida o limite e o registro antes de atualizar o componente persistente `simplestoragemanager:dimension_selection` no próprio upgrade; cancelar descarta as alterações locais.
- A GUI dimensional usa `dimensional_upgrade_gui.png`, linhas de `bg.png`/`bg_highlighted.png` renderizadas em 225x20 sem esticar as texturas, checkboxes redimensionadas da imagem completa de 20x20 para 10x10 e rolagem da lista com o scroller existente. Os botões Cancelar e Salvar ficam 5 pixels acima da posição anterior, com a instrução de seleção centralizada entre eles.
- O servidor reavalia a disponibilidade das conexões abertas a cada 20 ticks (um contador por menu) e envia atualizações apenas para conexões cujo estado mudou. Verificações ativas em operações reiniciam o contador; depósitos, retiradas e devoluções da grade consultam novamente o alcance e os chunks no momento da operação.
- Os títulos dos inventários conectados usam o `Component` retornado por `Nameable.getDisplayName()` da entidade de bloco conectada, com o nome do bloco como fallback. O componente segue sem conversão para texto bruto pelo payload até ser renderizado na GUI.
- Cliques manuais em inventários Sophisticated preservam o índice usando `InventoryHandler.insertItemOnlyToSlot`, com simulação e débito do cursor pela quantidade aceita na execução. A mochila Sophisticated é resolvida quando colocada como bloco (`BackpackBlockEntity`); seu handler exposto é associado ao `InventoryHandler` interno. Storage Drawers expõe somente os drawers físicos, em ordem estável por posição no grupo, mesmo quando o protótipo altera a ordem dinâmica do handler; o slot virtual interno 0 não aparece no terminal. A integração escreve pela API pública `IDrawerGroup`/`IDrawer`, sem usar o full scan de `insertItem`. A quantidade debitada é a diferença medida na contagem da gaveta. Os demais handlers continuam no caminho genérico; extração e shift mantêm suas regras com o índice traduzido para a ordem física estável.
- Em terminal com cadeado trancado, o início de arrasto manual sem Shift é autorizado apenas no inventário de origem da pilha do cursor ou em slot autorizado (inventário principal/hotbar ou Crafting Grid); qualquer outro início é cancelado antes da execução, sem consumir cursor ou alterar inventários.
- Snapshots de inventários conectados serializam a quantidade separadamente dos dados do item para não herdar o limite de 99 do codec Vanilla de `ItemStack`; a quantidade segue o limite inteiro do handler e o cliente também recebe a capacidade por slot para prever inserções em pilhas acima do máximo padrão.
- Na GUI, contagens acima de 999 são desenhadas em escala reduzida. Arrastos de inserção são confirmados automaticamente segundo as condições de término específicas de cada botão, sem esperar a liberação do botão quando a distribuição já terminou.
- Os três upgrades exibem uma instrução com o key mapping configurado para agachar; a descrição traduzida aparece enquanto essa tecla estiver pressionada. O inventário criativo também usa o key mapping de pegar item para clonar stacks e iniciar o arrasto de cópias. A cópia pela roda em inventários conectados é validada no servidor; o arrasto preenche somente os slots visitados e limita a cópia ao menor valor entre o máximo do item e o limite do slot.
- As descrições dos upgrades são quebradas em linhas usando a largura da fonte do cliente e preservam o texto traduzido.
- As conexões do terminal mantêm a ordem de conexão e um UUID próprio, persistidos junto da dimensão, posição e ID de registro do bloco âncora. Dados antigos recebem UUIDs estáveis durante a migração. O tipo salvo é apenas uma identificação da implementação resolvida; nenhum handler remoto é armazenado e a resolução continua respeitando a disponibilidade de chunks.
- No Shift-clique + arrasto iniciado no inventário do jogador, cada atualização da sequência visitada solicita ao servidor um plano visual cumulativo. O servidor usa a estratégia do adaptador efetivamente resolvido (wrapper `Container`, handler genérico ou integração específica) e simula as fontes em ordem sobre um estado virtual compartilhado. O cliente apenas renderiza as alterações recebidas; ao soltar, o servidor revalida e planeja/executa novamente, comparando o plano e os snapshots da prévia para cancelar com segurança se o estado remoto mudou. Como handlers remotos só podem ser simulados com segurança no servidor, a prévia depende da resposta de rede e não usa cálculo local aproximado como fallback. Se uma origem lembrada pelo cadeado estiver oculta pelo filtro de busca, o plano não pode ser representado fielmente na grade atual: a operação é cancelada com aviso, em vez de executar sem prévia ou usar o caminho legado. Handlers não reconhecidos pelas integrações são classificados como `MODDED_HANDLER` e consultados somente pelo contrato `insertItem(..., true)`; sem capability `IItemHandler`, a conexão permanece indisponível em vez de inferir uma estratégia ou carregar chunks.

## Decisões

1. A busca e parcial e nao diferencia maiusculas de minusculas. Ela compara o nome exibido do item e mostra uma unica secao `Resultados da Busca`.
2. Uma conexao de bau usa a estrutura de bau simples ou dupla retornada pelo estado do bau. Um bau so pode acrescentar o parceiro conectado, portanto tres baus produzem duas estruturas/inventarios.
3. Para blocos que nao sao baus, a descoberta em seis direcoes so agrupa vizinhos cujo `IItemHandler` de capability seja a mesma referencia (`==`). A decisao fica centralizada em `MultiblockDiscovery.isSameMultiblockInventory`; ela exige ambos os chunks carregados e executa no servidor, sem carregar chunks. Baús são tratados separadamente: só se conectam ao parceiro indicado pelo estado do baú, do mesmo tipo e com capability de inventário. A conexão e recusada se a caixa delimitadora inclusiva ultrapassar 5x5x5.
4. Ao quebrar um membro conectado, a conexão permanece salva e a seção exibe o alerta existente de conexão perdida. O instante da quebra é persistido; se continuar quebrada por uma hora de tempo real, a conexão e a seção são removidas. Indisponibilidade por outros motivos não inicia esse prazo. Se um inventário novo aparecer na posição antes do prazo, a conexão antiga é substituída pelo aviso verde já existente, centralizado e dispensável pelo botão X. Cada conexão também salva IDs de bloco por posição, permitindo rejeitar e remover membros substituídos quando os chunks estão carregados. Os campos são opcionais ao carregar dados antigos.
5. Nenhum chunk e carregado para verificar conexões existentes. A disponibilidade consulta o estado atual do chunk com `ServerChunkCache.hasChunk`; capabilities e block entities so sao acessadas depois dessa checagem.
6. O servidor e a fonte da verdade. O cliente envia pesquisa, rolagem e cliques; o servidor recalcula a visualizacao e valida cada operacao.
7. No modo básico, conexões em outra dimensão, fora do raio esférico de 500 blocos ou com chunks descarregados aparecem indisponíveis sem serem removidas. Essa disponibilidade é reavaliada periodicamente no servidor.
8. Ao validar conexões salvas cujos chunks já estão carregados, os membros são redescobertos pela regra atual e conexões antigas agrupadas apenas por adjacência são separadas. Os campos aditivos de proprietário e IDs de bloco são opcionais, mantendo a leitura de `TerminalSavedData` antigo.

## Formas de detecção de multiblocos

- Multibloco não-baú agora exige identidade de referência do `IItemHandler` entre cada par adjacente; capability ausente ou chunks descarregados não formam grupo.
- Baús continuam usando a ligação de parceiro do estado do bloco, separada da comparação de identidade.
- Conexões antigas são recalculadas durante a validação do servidor. Inventários independentes deixam de ser compostos em um único handler; um handler compartilhado é exposto uma única vez.

## Configuração de desenvolvimento e compatibilidade NeoForge

- O projeto principal usa NeoForge `21.1.262` em `build.gradle`; suas tarefas normais (`runClient`, `runServer`, `compileJava`, `build`) usam essa versão.
- O intervalo declarado para a dependência NeoForge está em `src/main/resources/META-INF/neoforge.mods.toml`, em `versionRange="[21.1.262,21.2)"`. Esse arquivo é copiado para `build/resources/main/META-INF/neoforge.mods.toml` por `processResources` e incluído no JAR. A faixa declara a compatibilidade pretendida; não garante compatibilidade funcional em cada versão.
- Para iniciar o cliente de desenvolvimento com NeoForge `21.1.262`, use a configuração Gradle `Minecraft Client (NeoForge 21.1.262)` no IntelliJ IDEA ou execute a tarefa Gradle `runClient` no projeto principal. Não use uma configuração `Application` que inicialize `net.neoforged.devlaunch.Main` diretamente: ela depende de argumentos gerados pelo Gradle em `build/moddev` e falha quando esses arquivos ainda não existem, por exemplo após `clean`. A configuração `.run/Minecraft Client (NeoForge 21.1.262).run.xml` aponta para a raiz do projeto e usa o `GRADLE_USER_HOME` normal do ambiente. O código-fonte, recursos e testes são os mesmos usados por `compileJava`, `test` e `build`.
- As dependências de Sophisticated Core, Sophisticated Storage, Sophisticated Backpacks e Storage Drawers são `compileOnly`. A propriedade `sophisticated_backpacks_version=3.26.14.2222` fixa a versão usada para compilar a integração da mochila; nenhum desses mods é incorporado ao JAR do Simple Storage Manager.

## Propriedade, upgrades e proteção de inventários

- O proprietário é o UUID persistente do jogador que conecta o primeiro inventário ao terminal; terminais sem proprietário não permitem mutações remotas. Pesquisa e visualização continuam abertas. Cliques remotos, depósitos, shift-cliques e devoluções de crafting verificam a propriedade no servidor, e mensagens novas estão em `message.simplestoragemanager.terminal_not_owner` e `message.simplestoragemanager.terminal_not_owner_connection`.
- `protectConnectedInventories` é uma opção de configuração SERVER, padrão `false`; com `true`, o servidor recusa conectar a outro terminal qualquer membro carregado de um inventário já vinculado. A recusa usa `message.simplestoragemanager.inventory_already_connected`.
- A identidade do bloco por membro é persistida como ID de registro, sem referência em cache. Quebras mantêm o membro vinculado durante até uma hora; substituições de outro tipo também invalidam o vínculo quando verificadas. Conexões antigas sem esses IDs são carregadas e recebem a identidade atual na primeira redescoberta; uma substituição anterior a essa migração não pode ser distinguida. Blocos idênticos substituídos por mecanismos que não disparam `BlockEvent.BreakEvent` também não podem ser distinguidos apenas por posição e ID de registro.
- Os testes JUnit sem dependência de runtime Minecraft cobrem a regra do nível efetivo para upgrade nível 2 vazio e a autorização por UUID. Persistência NBT e eventos de bloco requerem validação em jogo.

## Layout

As coordenadas são relativas ao canto superior esquerdo da textura. O item usa deslocamento de `(1, 1)` dentro do slot de 18x18. O recorte da area principal e aplicado com scissor depois de somar `leftPos` e `topPos`.

| Elemento | SMALL | BIG |
| --- | --- | --- |
| Imagem | 216x230 | 216x342 |
| Area principal | (31,23)-(192,65) | (31,23)-(192,177) |
| Trilho | (198,23)-(210,141) | (198,23)-(210,141) |
| Pesquisa | (103,8)-(192,19) | (103,8)-(192,19) |
| Upgrade 3/2/1 | (6,5), (6,23), (6,41) | iguais |
| Miniatura do jogador | (24,70)-(74,141) | (24,181)-(74,252) |
| Armadura | (7,70), (7,88), (7,106), (7,124) | (7,181), (7,199), (7,217), (7,235) |
| Mao secundaria | (75,124) | (75,235) |
| Crafting | origem (103,80), passo 18 | origem (103,191), passo 18 |
| Resultado | (175,98) | (175,209) |
| Botao X | (156,70) | (156,182) |
| Cadeado (10x10) | (83,70) | (83,182) |
| Inventario principal | origem (31,147), 3x9 | origem (31,258), 3x9 |
| Hotbar | origem (31,205), 9 slots | origem (31,316), 9 slots |

Todos os valores ficam em `TerminalScreenLayout`. A moldura, o nome, os slots, os itens e os textos de estado das secoes sao desenhados dentro do scissor da area principal.

O botão X usa as imagens completas, redimensionadas para 8x8 pixels, em `assets/simplestoragemanager/textures/screens/cross_button.png` e `assets/simplestoragemanager/textures/screens/cross_button_highlighted.png`. Seu tooltip usa `gui.simplestoragemanager.storage_manager_gui.clear_crafting`. O botão X nos avisos verdes de substituição exibe a tooltip traduzida `screen.simplestoragemanager.dismiss_notice`. Cada seção indisponível mostra no canto superior direito um ícone de alerta de 10x10 pixels, usando as imagens completas `assets/simplestoragemanager/textures/screens/report_button.png` e `assets/simplestoragemanager/textures/screens/report_button_highlighted.png`; seu tooltip traduzido quebra automaticamente em linhas. O cadeado usa `unlocked_button.png`, `unlocked_button_highlighted.png`, `locked_button.png` e `locked_button_highlighted.png`: cada textura original de 20x20 é desenhada inteira em 10x10, com amostragem nearest-neighbor (sem recorte). O hover e o estado atual escolhem conjuntamente a textura; as tooltips traduzidas `screen.simplestoragemanager.inventory_lock.*` mostram a descrição detalhada ao pressionar Shift e quebram automaticamente em linhas. Os itens nos slots de armadura, mão secundária, crafting, resultado e inventário/hotbar do jogador mostram os tooltips padrão de ItemStack. A miniatura de crafting tem nove slots contíguos, com passo 18, e um slot de resultado nos pontos indicados na tabela. Os itens da grade são persistidos apenas durante a vida do menu; ao fechá-lo, são devolvidos ou derrubados.

## Menus dropdown dos botões de inventário

- Os botões de ação rápida das seções (organizar e filtro) abrem um menu dropdown com clique direito; clique esquerdo seleciona a opção, clique fora ou Esc fecha, e a seleção toca o som de UI.
- O menu de filtro ("Permitir"/"Negar") tem as opções como linhas selecionáveis, com marcação ✓ na opção escolhida.
- O menu de organização ("Organizar por:") tem a primeira linha como título não selecionável e as opções ("Nome", "Quantidade", "Tipo de Item", "Mod") como linhas selecionáveis. A opção selecionada é usada quando o jogador clica com o botão esquerdo no botão de organizar daquela seção.

## Auto-organização de inventário conectado

- Clicar com o botão esquerdo no botão de organizar de uma seção executa a auto-organização apenas naquele inventário conectado, usando a opção atualmente selecionada no menu dropdown ("Nome", "Quantidade", "Tipo de Item" ou "Mod"). Clique direito no botão apenas abre o dropdown; clicar em uma opção do dropdown não organiza imediatamente — apenas seleciona. Clique com o botão direito do mouse sobre o botão de organização não faz nada além de abrir o dropdown.
- A organização é feita no servidor sobre o `IItemHandler` do inventário conectado: todas as pilhas são extraídas, ordenadas e reinseridas empacotadas a partir do primeiro slot (linhas superiores), deixando slots vazios no final do inventário. Nenhum outro inventário é afetado e a organização não altera a quantidade de itens de nenhuma pilha, apenas move as pilhas entre os slots.
- A organização não é considerada extração nem inserção, portanto não está sujeita às regras de filtro de destino nem às regras de inserção/extração de itens.
- A organização é validada e aplicada no servidor, e a interface é sincronizada após a validação e aplicação. Se não for possível realizar a organização (inventário indisponível, erro de sincronização, erro de servidor), uma mensagem traduzível (`message.simplestoragemanager.organize_failed`) é exibida no chat do player.
- A configuração de organização (opção selecionada, direção atual e se já foi organizado pelo menos uma vez) é persistida por conexão em `TerminalSavedData` (NBT `OrganizeConfigs`), sobrevivendo a fechar/reabrir a GUI e reiniciar o jogo.
- Ao trocar a opção selecionada no dropdown de um inventário já organizado, a alternância entre crescente e decrescente continua a partir do estado atual daquele inventário.

## Filtro de inventário

- Cada botão de filtro está conectado ao inventário da seção onde está localizado; clique esquerdo ativa o modo de edição de filtro para aquele inventário específico, e clicar novamente salva a configuração e desativa o modo de edição.
- O modo de edição é 100% client side: apenas ao salvar (clicar no botão de filtro) as alterações são enviadas ao servidor via `TerminalFilterSavePayload` para validação e persistência.
- O dropdown do botão (Permitir/Negar) define se o inventário permitirá apenas os itens configurados no filtro (quantidades ignoradas) ou proibirá aqueles itens de serem inseridos. Com o filtro vazio, nenhuma restrição é aplicada, independentemente da opção selecionada.
- Cada inventário conectado tem sua própria configuração de filtro, independente dos outros; vários inventários podem estar em edição simultaneamente sem afetar uns aos outros.
- Ao ativar a edição, os slots da seção exibem cópias dos itens configurados (ou ficam vazios na primeira ativação). Clique com item no cursor adiciona uma cópia de uma unidade ao slot sem consumir o cursor nem afetar o inventário físico; clique sem item limpa o slot.
- A configuração persiste após fechar a GUI, salvar/recarregar o mundo e reiniciar o jogo. Fechar a GUI sem salvar descarta as alterações não salvas e preserva a configuração anterior. Clique direito no botão de filtro não ativa nem desativa o modo de edição.
- O servidor valida o filtro apenas quando o player salva; caso ocorra erro de validação, a configuração previamente salva permanece como persistente.

## Escala da GUI

A escolha acontece no `init()` da tela usando `Minecraft.getInstance().options.guiScale().get()`, sem usar a escala efetiva de `Window`:

| Valor configurado | Textura |
| --- | --- |
| 0 (Auto) | SMALL |
| 1 | BIG |
| 2 | BIG |
| 3 | SMALL |
| 4 | SMALL |

Essa tabela e implementada por `TerminalScaleChoice`. A variante e escolhida novamente apenas na abertura da tela.

## IDs e texturas

| Recurso | ID/caminho                                                                |
| --- |---------------------------------------------------------------------------|
| Terminal | `simplestoragemanager:storage_manager`                                    |
| Upgrade 1 | `simplestoragemanager:infinity_upgrade`                                   |
| Upgrade dimensional | `simplestoragemanager:dimensional_infinity_upgrade`                       |
| Upgrade multidimensional | `simplestoragemanager:multidimensional_infinity_upgrade`                   |
| Menu | `simplestoragemanager:storage_manager_gui`                                |
| Fundo SMALL | `assets/simplestoragemanager/textures/screens/storage_manager_gui.png`    |
| Fundo BIG | `assets/simplestoragemanager/textures/screens/storage_manager_gui_big.png` |
| Slot vazio | `assets/simplestoragemanager/textures/screens/slot.png`                   |
| Scroller | `scroller.png` e `scroller_disabled.png`                                  |
| Botao X | `cross_button.png` e `cross_button_highlighted.png`                       |

## Testes

Os testes JUnit cobrem busca parcial sem diferenciar maiusculas, regras de deposito, toggle, alcance básico pelo centro do bloco usando distância ao quadrado, coordenadas/rolagem, tabela de escala, extração direita arredondada para cima com teto do limite do item, dependências de remoção dos upgrades em ordem e o planejamento puro de distribuição/capacidade do arrasto. O build foi validado com `gradlew test build --no-configuration-cache`; a inclusão acidental de classes/recursos Sophisticated ou Storage Drawers no JAR foi verificada e não ocorreu. Os caminhos de rede, capabilities, baús duplos e as integrações em execução dependem do ambiente Minecraft e devem ser conferidos em `runClient`; os testes manuais das integrações Sophisticated Backpacks e Storage Drawers ainda estão pendentes.

Matriz de validação manual pendente: repetir as operações de clique, shift-clique e shift-arrasto em inventário vanilla de 64, inventário de mod de 64 e inventário de mod com slot acima do limite do item; fazer cada teste com o cadeado destrancado e trancado, em SMALL/BIG e singleplayer/multiplayer. Confirmar inserção, troca, ordem de pilhas parciais/vazias, simulação antes da mutação, devolução à origem e encerramento do arrasto sem destino. A prévia do cliente é apenas visual; o servidor decide e confirma a operação.

## Upgrades e seleção de dimensões

`UpgradeAccessRules` centraliza o nível efetivo, as regras de distância e dimensão e a validação da seleção de no máximo cinco dimensões registradas. `StorageManagerGUIMenu` consulta essas regras antes de expor ou operar conexões, mantendo a validação de chunk carregada para todos os níveis.

`DimensionalUpgradeSelectionScreen` mostra as dimensões recebidas do servidor, mantém alterações apenas localmente até Salvar e descarta-as ao Cancelar. A lista usa o scroller existente e a roda do mouse. Abertura, salvamento e resposta de validação são tratados por `DimensionalUpgradeOpenPayload`, `DimensionalUpgradeSavePayload` e `DimensionalUpgradeSaveResultPayload`. As chaves de tradução da tela estão em `lang/en_us.json` e `lang/pt_br.json`, sob `screen.simplestoragemanager.dimension_selector.*`.

Os testes JUnit cobrem o nível efetivo, as regras de distância e dimensão dos níveis 0–3 e a validação de seleção vazia, limite de cinco, duplicatas e dimensões não registradas.

## Clique simples, shift-click, arrasto e arrasto sem shift — funcionamento atual

Esta seção registra o funcionamento atual de cada clique/arrasto contra inventários conectados, com foco nos cliques de mouse (esquerdo e direito) e em arrastos, com e sem shift, para inserção e extração.

### Clique simples com item no cursor (sem shift)

- Botão direito (1): se o item do slot for diferente do item do cursor → swap (1:1, exigindo pilha única). Se for o mesmo item → insere 1 unidade (limitando ao espaço disponível).
- Botão esquerdo (0): se o item do slot for diferente do item do cursor → swap, permitido apenas quando a pilha do slot tem quantidade ≤ limite do item (`existing.getCount() <= existing.getMaxStackSize()`); caso contrário nada acontece. Se for o mesmo item → consulta a capacidade real do slot (`simulatedCapacity` para Sophisticated Storage/Storage Drawers, `insertRemoteSlot` para handlers genéricos): com espaço → insere/soma as quantidades (excedente permanece no cursor); sem espaço → nada (sem swap, sem inserção).

### Clique simples com shift (shift-click)

- Extração com shift: rota dedicada (`shiftRemoval`) que envia intenção ao servidor e executa a extração conforme as regras de extração.

### Arrasto sem shift

- Início: `StorageManagerGUIMenu.handleRemoteDrag` (servidor), disparado pelo payload do cliente quando o arrasto começa sem shift.
- Botão esquerdo: `requested = carried.getCount()`; botão direito: `requested = min(carried.getCount(), targets)`.
- Prioridade de origem (cadeado trancado): com cursor com registro de origem e nenhum destino sendo a origem → devolução à origem via `insertIntoRememberedOrigins` antes da distribuição; excedente segue para os destinos visitados.
- Distribuição: `ManualInsertionRules.planDrag`; integrações específicas (Sophisticated/Storage Drawers) usam os slots manuais quando suportados.

### Arrasto com shift (shift-drag)

- `handleRemoteShiftDrag` / `ShiftDragPlanner`: devolução primeiro aos origins lembrados, depois destino remoto por tipo de item; preview cumulativa via `TerminalShiftDragPreviewPayload`.

### Extração (clique e arrasto de saída)

- Sem shift: clique esquerdo extrai limite do item (`extractionCount`); clique direito extrai metade (`rightClickAmount`).
- Com shift: extração via rota shift.

## Edição dos títulos das seções

- A edição de título de seção é local no cliente: duplo clique no título dentro da área de conteúdo (`handleTitleClick` → `startTitleEdit`), com backup em `editingBackupName` e `EditBox` posicionado por `updateTitleEditBoxRect`.
- Enquanto edita, o desenho usa o `EditBox` e a rolagem é ignorada; Enter confirma (`confirmTitleEdit`) e Esc cancela (`cancelTitleEdit`), repassando teclas ao `EditBox`.
- Ao confirmar, o rótulo local em `view.sections()` é atualizado (`setSectionLabel`) e o cliente envia `TerminalRenamePayload(sectionIndex, newName)`.
- No servidor, `handleRename` valida permissão/índice/conexão, persiste via `TerminalSavedData.putCustomName`, atualiza o view e reenvia aos viewers do mesmo terminal (`broadcastRenameToTerminalViewers`).
- O nome customizado prevalece sobre o fallback do bloco no rótulo exibido (`effectiveLabel`) e é persistido em `TerminalSavedData.customNames` (NBT `CustomNames`); nomes vazios não são guardados.

## Comportamentos esperados para cada click que interage com a área principal da GUI do terminal

1. Clique simples esquerdo (pegar, depositar, trocar).
2. Clique simples direito (mesmo que o esquerdo, porém limitado a um item por operação, metade ao extrair).
3. Shift-clique no inventário do player, incluindo hotbar, envia a pilha toda ao primeiro slot vazio, priorizando pilhas parciais.
4. Shift-clique no inventário conectado envia a pilha inteira (ou limitado ao máximo do item em slots que aceitam mais de 64) para o inventário do player primeiro, depois pra hotbar, ou cancela.
5. Shift-clique + arrasto em inventário conectado envia todas as pilhas sobre as quais o mouse passar, respeitando as regras de shift-click acima.
6. Shift-clique + arrasto no inventário do player envia cada pilha para o primeiro slot válido de algum inventário conectado ao terminal:
   - Primeiro tenta enviar para pilhas parciais do mesmo tipo que ainda possuem espaço para inserção.
   - Depois tenta enviar para o primeiro slot vazio.
   - O "espaço para inserção" é definido primeiro pelo limite máximo do próprio item (64, 16 ou 1) e depois é definido pelo limite máximo do próprio slot (64 ou mais). Ou seja, nos slots convencionais, o limite do item determina quando a pilha está cheia. Nos slots que aceitam mais de 64 itens, o limite do slot determina quando a pilha está cheia, desde que o item seja empilhável.
   - Um "slot válido" é definido pelo fato de haver espaço para inserção, independente do limite máximo de itens que cabe no slot. Ou seja, se a quantidade de itens presente no slot for menor que os limites (primeiro do item e depois do slot), então há espaço para inserção.
   - Fórmula pra calcular o espaço para inserção: if (slotMax == 64 && itemMax == 64 && itemCount < 64) || (slotMax == 64 && itemMax == 16 && itemCount < 16) || (slotMax > 64 && itemMax > 1 && itemCount < slotMax) {return true} else {return false};
7. Arrasto esquerdo sem Shift em inventário conectado distribui os itens pelos slots e equaliza as quantidades, caso o cadeado esteja destrancado.
   - Com cadeado trancado, esta operação só funciona no inventário principal do player, na hotbar e no inventário de origem do item.
   - Caso não haja inventário de origem, então o funcionamento é igual à quando o cadeado está destrancado.
8. Arrasto direito sem Shift em inventário conectado distribui apenas um item em cada slot sobre os quais o mouse passar, caso o cadeado esteja destrancado.
   - Com cadeado trancado, esta operação só funciona no inventário principal do player, na hotbar e no inventário de origem do item.
   - Caso não haja inventário de origem, então o funcionamento é igual à quando o cadeado está destrancado.
9. Qualquer clique com cadeado trancado deve ter os mesmos comportamentos descritos acima, porém sempre priorizando o inventário de origem:
   - O "inventário de origem" é o inventário conectado ao terminal de onde o item saiu em uma extração anterior.
   - Clique esquerdo fora do inventário de origem com item no cursor enquanto o cadeado está trancado, envia a pilha para o primeiro slot do inventário de origem com espaço para inserção, priorizando pilhas parciais.
   - Clique direito faz o mesmo, porém um item de cada vez.
10. Somente quando o jogo estiver no modo criativo:
   - Clicar com a roda do mouse sobre um item com o cursor vazio cria uma cópia do stack completo do item que tá no slot e envia-o para o cursor.
   - Clicar em slot vazio com item no cursor (com ou sem arrasto), criar cópias do stack, enviando-as aos slots sobre os quais o mouse passar (limitando-se primeiro à quantidade máxima do item e depois à do slot). Passar sobre slot que já contenha item durante a operação de arrasto ignora o slot cheio, independente do item presente no slot.
   - Clicar em slot com item, tendo item no cursor, não faz nada.
   - O clique com a roda do mouse não é afetado pelo cadeado, ele funciona da mesma forma em todo tipo de slot, independente do cadeado.
   - Somente nos seguintes slots da GUI que ele vai ser limitado: slots de upgrades (não insere itens que não sejam upgrades, limitado a 1 em cada slot), slots de armadura (só aceita armaduras, limitado a 1 por slot), slot de resultado de receitas (é slot de saída). Todos os outros slots funcionam como descritos acima.
