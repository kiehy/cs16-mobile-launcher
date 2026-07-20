# Patch: Launcher CS 1.6 (Notícias + Servidores + Resolução + Discord)

Este pacote contém só os arquivos **novos** e **modificados** em cima do
`xash3d-fwgs-master/android`. Não é o projeto inteiro — é pra copiar por cima
do seu clone do `xash3d-fwgs`.

## Como aplicar

1. Pegue o `xash3d-fwgs` (o mesmo que você me mandou, com submódulos
   inicializados — vai precisar dele completo pra compilar o engine nativo).
2. Copie o conteúdo da pasta `android/` deste patch por cima da pasta
   `android/` do seu `xash3d-fwgs`, mantendo a estrutura de pastas:

```bash
cp -r patch/android/* /caminho/do/seu/xash3d-fwgs/android/
```

   Isso vai sobrescrever `MainActivity.kt`, `Game.kt`, `activity_main.xml`,
   `nav_graph.xml`, `strings.xml`, `app_preferences.xml` e
   `game_preferences.xml` (todos com as mudanças aditivas descritas abaixo),
   e adicionar os arquivos novos (fragments, adapters, layouts, etc).

3. Abra o projeto no Android Studio (pasta `android/`) e deixa sincronizar o
   Gradle.
4. Compile e roda num device/emulador.

## O que foi adicionado/alterado

**Nova aba "Servidores"** (`ui/servers/ServersFragment.kt`)
- Lista fixa em `model/ServerEntry.kt` — **edite esse arquivo** pra colocar
  o IP/porta reais dos seus servidores.
- Ao tocar num servidor, conecta automaticamente no CS 1.6 instalado usando
  `+connect ip:porta gs` (protocolo GoldSource, pra falar com os servidores
  de PC).
- Se o CS 1.6 não estiver instalado, mostra um diálogo oferecendo baixar o
  cliente.

**Nova aba "Notícias"** (`ui/news/NewsFragment.kt`)
- Busca uma lista de notícias de uma URL JSON configurável em
  Configurações → "URL das notícias" (é a URL do `news.json` que o bot do
  Discord no seu VPS vai publicar).
- Puxar pra baixo atualiza a lista.

**Config de resolução** (`game_preferences.xml` + `Game.kt`)
- Novo seletor de resolução na tela de configurações de cada jogo. Se não
  for "Automática", passa `-width` e `-height` pro engine.

**Link do Discord** (`app_preferences.xml` + `AppSettingsPreferenceFragment.kt`)
- Em Configurações → "Link do Discord", edite a URL do convite.
- Em Configurações → "Entrar no Discord", abre o link no navegador/app do
  Discord.

**Navegação por abas** (`activity_main.xml` + `MainActivity.kt` + `nav_graph.xml`)
- Adicionei uma barra de navegação inferior com 3 abas: Jogar, Servidores,
  Notícias. As configurações continuam acessíveis pelo ícone de engrenagem
  no topo (como já era antes).

## Antes de compilar, edite:

1. `android/app/src/main/java/su/xash/engine/model/ServerEntry.kt` — troque
   os IPs/portas de exemplo pelos servidores reais da sua comunidade.
2. Depois de instalar o app, vá em Configurações e preencha:
   - **URL das notícias**: `https://SEU_DOMINIO/news.json` (a que o bot do
     Discord está servindo)
   - **Link do Discord**: o convite real do seu servidor

## Observações

- Não rodei o build aqui porque este ambiente não tem o Android SDK/NDK nem
  os submódulos do engine — só pude validar sintaxe (XML válido, chaves e
  parênteses batendo nos arquivos Kotlin). Primeira coisa a fazer é abrir no
  Android Studio e ver se sincroniza/compila; me manda qualquer erro que
  aparecer que eu ajusto.
- O nome/pacote dos jogos (`cstrike`/`czero`) segue o padrão que o próprio
  `xash3d-fwgs` já usa pra reconhecer o CS 1.6 — não precisa mudar nada
  nisso.
