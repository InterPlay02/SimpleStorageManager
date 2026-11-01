# Simple Storage Manager - documentacao do Mod

## Arquitetura atual

- `StorageManagerGUIMenu` mantém os slots do jogador, armaduras, mão secundária, upgrades e a grade temporária 3x3 com resultado vanilla. Inventários conectados continuam fora dos slots do menu.
- Inventários conectados nunca são representados por `Slot` do menu. O servidor os sincroniza em snapshots próprios; o snapshot contém todos os slots conectados, inclusive os que estão fora da área visível, para que a previsão use o mesmo estado-base da operação.
- Cliques e arrastos em inventários conectados usam `TerminalStorageClickPayload`; operações em lote carregam um número de sequência e o cliente mantém a previsão até receber um único `TerminalViewPayload` de confirmação com os slots remotos e locais finais. Snapshots com sequência anterior são ignorados. Arrastos enviam a lista ordenada de alvos ao soltar, sem pacote durante o movimento.
- O botão de cadeado usa `TerminalLockTogglePayload`, que envia somente a intenção de alternar. O estado é decidido no servidor, validado por `TerminalOwnershipRules` e sincronizado no snapshot da GUI; não proprietários ainda podem ver o estado e a tooltip.
- Com o cadeado ativado, cada extração de inventário conectado registra em memória de sessão o jogador, terminal, item e componentes, conexão, slot, quantidade e ordem FIFO. Uma inserção correspondente tenta devolver primeiro a quantidade pendente ao inventário de origem, revalidando a conexão e o chunk; dentro dele, completa pilhas compatíveis antes de ocupar slots vazios. Em clique manual na própria origem, porém, o destino é o slot clicado; ao clicar em outra conexão, a devolução à origem tem prioridade. Se a origem estiver indisponível ou sem espaço, o restante segue o caminho normal. A memória não usa handlers em cache, não é salva em NBT e é descartada ao destrancar, ao jogador sair e ao servidor parar.
- O estado do cadeado fica no `TerminalSavedData` persistente por UUID do terminal. Ao reabrir, `sendView` envia o valor atual junto do snapshot; a GUI escolhe a textura a partir desse valor e do hover atual. O estado padrão de terminais antigos ou sem campo salvo é destrancado.
- Shift+clique e Shift+clique+arrasto esquerdo em slots do jogador começam uma previsão local e enviam um único lote ao soltar (um slot no clique simples). A quantidade é lida do stack no servidor. No cadeado trancado, o servidor devolve primeiro à origem, completando pilhas compatíveis antes de slots vazios; o restante segue a inserção normal. No shift-arrasto, os slots de origem são processados uma vez, na ordem visitada, e cada tipo de item reutiliza um único destino remoto durante a sequência para evitar espalhar inserções repetidas em vários slots. O servidor revalida conexões e chunks, e confirma de uma vez os slots locais e remotos finais.
- Ao equipar uma peça nos slots de armadura da GUI, o menu toca o som definido pelo item somente após confirmar a alteração no servidor.
- Quando o snapshot não contém conexões nem avisos, a área principal mostra uma instrução centralizada para conectar o primeiro inventário.
- Shift+clique de extração calcula no servidor o espaço disponível no inventário do jogador antes de extrair do inventário conectado; a simulação não altera os slots reais do jogador.
- Seções indisponíveis recebem fundo escuro e faixas diagonais laranjas, com uma área escura atrás do aviso central. O recorte do conteúdo continua aplicado pelo scissor principal.
- A tela desenha tooltips dos slots manualmente. A tooltip de item remoto só aparece sobre um slot remoto válido dentro da área principal da variante atual; a hit-test dos slots locais também usa suas posições customizadas, incluindo a variante BIG, sem suprimir tooltips de slots do jogador, upgrades ou crafting.
- A pesquisa usa `TerminalViewRequestPayload` e é executada no servidor sobre todos os slots de todas as conexões.
- O ItemStack guarda apenas `terminal_id`. Conexões, membros de multiblocos, membros quebrados, avisos, proprietário e estado persistente do cadeado ficam em `TerminalSavedData` no save do mundo.
- `TerminalSavedData` também persiste o UUID do proprietário do terminal. A propriedade é definida na primeira conexão de inventário; outros jogadores podem consultar a GUI, mas operações pelo terminal e alterações de conexões são recusadas no servidor.
- O arquivo de configuração SERVER `simplestoragemanager-server.toml` define `protectConnectedInventories` (padrão `false`). Quando ativo, impede conectar um inventário atualmente vinculado a outro terminal.
- A grade e o resultado de crafting existem no menu e são sincronizados pelo sistema normal de slots. O servidor consulta as receitas `CRAFTING` e usa `ResultSlot` para consumir ingredientes e devolver sobras de receitas especiais.
- Shift+clique dos slots da grade, do resultado, de armadura e da mão secundária tenta primeiro completar pilhas correspondentes no inventário principal e hotbar do jogador; depois usa slots vazios do inventário principal da esquerda para a direita, slots vazios da hotbar da esquerda para a direita e, por fim, slots compatíveis dos inventários conectados. O resultado de crafting só é fabricado uma vez por operação e somente enquanto todo o produto puder ser inserido nesses destinos.
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
- Cliques manuais nos inventários conectados operam no índice clicado, com regras de clique esquerdo/direito e simulação de capacidade no servidor. Extrações em handlers modificáveis atualizam o slot e registram a origem; handlers não modificáveis usam `extractItem`. Na inserção do clique, handlers modificáveis gravam o slot exato com `setStackInSlot`; handlers não modificáveis usam `insertItem` no índice clicado. Trocas de itens diferentes só são feitas quando o handler permite a substituição segura do slot.
- Arrastos manuais esquerdo/direito preservam os slots visitados: o esquerdo divide a quantidade entre os slots aceitos e o direito coloca um item por slot. Para handlers modificáveis de mods, a inserção remota é gravada no índice visitado para evitar que o handler redirecione ao primeiro slot vazio; wrappers vanilla mantêm o caminho existente para preservar suas notificações de alteração. Arrastos criativos copiam a quantidade do cursor somente após validação de modo e capacidade no servidor.
- No arrasto esquerdo, esgotar a prévia do cursor não encerra a operação enquanto a média ainda permitir mais de um item por slot elegível; a equalização pode se recalcular ao visitar novos slots. A operação termina quando a distribuição chega a no máximo um item por slot, ou quando o cursor se esgota no arrasto direito. Ao finalizar, usa a mesma confirmação no servidor que o término normal do arrasto.
- Snapshots de inventários conectados serializam a quantidade separadamente dos dados do item para não herdar o limite de 99 do codec Vanilla de `ItemStack`; a quantidade segue o limite inteiro do handler e o cliente também recebe a capacidade por slot para prever inserções em pilhas acima do máximo padrão.
- Na GUI, contagens acima de 999 são desenhadas em escala reduzida. Arrastos de inserção são confirmados automaticamente segundo as condições de término específicas de cada botão, sem esperar a liberação do botão quando a distribuição já terminou.
- Os três upgrades exibem uma instrução com o key mapping configurado para agachar; a descrição traduzida aparece enquanto essa tecla estiver pressionada. O inventário criativo também usa o key mapping de pegar item para clonar stacks e iniciar o arrasto de cópias.
- As descrições dos upgrades são quebradas em linhas usando a largura da fonte do cliente e preservam o texto traduzido.

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

Os testes JUnit cobrem busca parcial sem diferenciar maiusculas, regras de deposito, toggle, alcance básico pelo centro do bloco usando distância ao quadrado, coordenadas/rolagem e a tabela de escala. Os caminhos de rede, capabilities, baus duplos e persistencia dependem do ambiente Minecraft e devem ser conferidos em `runClient`.

Para validar as interações recentes, repetir clique esquerdo/direito, shift-clique e arrasto normal e Shift em inventários vanilla e de mods, com o cadeado aberto e trancado. No cadeado trancado, verificar retorno a pilhas parciais da origem antes de slots vazios; em arrastos normais, confirmar que os itens ficam nos slots visitados, inclusive em handlers de mods.

Casos de teste manual para o crafting e o botão X:

1. Abrir com a grade vazia e confirmar que nenhum resultado é exibido.
2. Fazer uma receita com sobras (por exemplo, baldes de leite) e confirmar que cada recipiente é devolvido.
3. Shift-clique no resultado com materiais e espaço suficiente; confirmar que fabrica o máximo possível.
4. Com inventário principal cheio, limpar a grade; confirmar que usa primeiro a hotbar e depois os inventários conectados.
5. Com todos os destinos cheios, clicar X; confirmar que os itens permanecem na grade, inclusive sobras parciais.
6. Fechar a GUI com itens na grade e espaço no inventário; confirmar a ordem de devolução.
7. Fechar a GUI sem espaço; confirmar que os itens caem perto do jogador e saem da grade.
8. Repetir os testes nas variantes SMALL e BIG, verificando a posição do X e dos slots.

## Upgrades e seleção de dimensões

`UpgradeAccessRules` centraliza o nível efetivo, as regras de distância e dimensão e a validação da seleção de no máximo cinco dimensões registradas. `StorageManagerGUIMenu` consulta essas regras antes de expor ou operar conexões, mantendo a validação de chunk carregada para todos os níveis.

`DimensionalUpgradeSelectionScreen` mostra as dimensões recebidas do servidor, mantém alterações apenas localmente até Salvar e descarta-as ao Cancelar. A lista usa o scroller existente e a roda do mouse. Abertura, salvamento e resposta de validação são tratados por `DimensionalUpgradeOpenPayload`, `DimensionalUpgradeSavePayload` e `DimensionalUpgradeSaveResultPayload`. As chaves de tradução da tela estão em `lang/en_us.json` e `lang/pt_br.json`, sob `screen.simplestoragemanager.dimension_selector.*`.

Os testes JUnit cobrem o nível efetivo, as regras de distância e dimensão dos níveis 0–3 e a validação de seleção vazia, limite de cinco, duplicatas e dimensões não registradas.
