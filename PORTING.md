# 26.1.2 NeoForge 移植说明

本目录是 **YawningNekoAPI**（mod id: `yawning_neko_api`）的 Minecraft **26.1.2 / NeoForge 26.1.2.76** 移植工程。

## 当前状态

- [x] 工程骨架：ModDevGradle 2.0.140 + Java 25 工具链 + 版本目录 + `neoforge.mods.toml` 模板
- [x] **构建链路已验证打通**（用最小占位类实测）：`createMinecraftArtifacts` → `compileJava` → `processResources` → `jar` 全部成功，产出 `build/libs/yawning_neko_api-1.0.5.jar`
- [x] 1.20.1 的源码/资源已复制进来作为移植起点
- [x] 24 个 java 文件全部适配 NeoForge 26.1.2 API
- [x] **逐文件编译验证**：用 JDK 25 的 `javac` 直接对着真实的 26.1.2 构件
      （`build/moddev/artifacts/minecraft-patched-26.1.2.76.jar` + `neoforge-26.1.2.76-universal.jar` +
      `~/.gradle/caches/modules-2/files-2.1/**`）编译，**0 errors**（仅 1 条与原版一致的 unchecked 警告）
- [ ] 在目标目录跑一次真实的 `gradlew build` 做最终确认（含 Mixin 注解处理 / 资源打包）

## 构建

```bash
./gradlew build
```

### 已就位的前置条件

- **JDK 25**：位于 `~/.jdks/openjdk-25.0.2`（26.1.2 要求 Java 25）。
- **NeoForge 构件**：已缓存到本机 Gradle 缓存
  （`~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.76/` 含 `userdev` 与 `sources`；
  `~/.gradle/caches/neoformruntime/` 含 `minecraft_26.1.2_client/server` 与补丁结果）。
  **构件已缓存，日常构建不再需要代理。**
- `gradle.properties` 里目前保留了下载用的本地代理（`systemProp.*.proxyHost=127.0.0.1:51081`）。
  代理关闭后如果构建报网络错误，删掉这 5 行即可（或当作离线构建的开关）。
- 参考用产物：`build/moddev/artifacts/minecraft-patched-26.1.2.76.jar` 与 `-sources.jar`（可查 vanilla API）、
  `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.76/*-sources.jar`（NeoForge API）。
- Mixin 由 `neoforge-26.1.2.76-userdev.jar` 的 `config.json` 提供
  （`net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7`、`io.github.llamalad7:mixinextras-neoforge:0.5.4`），
  ModDevGradle 会自动带上编译期依赖，`build.gradle` 无需额外声明。

## 已实测确认的 26.1.2 API 变化

| 1.20.1（Forge） | 26.1.2（NeoForge） |
|---|---|
| `net.minecraft.resources.ResourceLocation` | **`net.minecraft.resources.Identifier`**（`Identifier.fromNamespaceAndPath(ns, path)`；`tryParse` 仍存在且返回可空） |
| `@Mod` 无参构造 + `FMLJavaModLoadingContext` | **构造注入 `(IEventBus modEventBus, ModContainer modContainer)`** |
| `MinecraftForge.EVENT_BUS` | **`NeoForge.EVENT_BUS`**（`modEventBus` 仍用于注册表/加载期事件） |
| `@Mod.EventBusSubscriber` | `@EventBusSubscriber`（只有 `modid`/`value`（Dist 数组），没有 `bus` 参数：它固定挂 NeoForge 游戏总线） |
| `LivingHurtEvent` | **`LivingIncomingDamageEvent`**，伤害改为 **`DamageContainer`** 管线（`getContainer()` / `addReductionModifier()` / `setAmount()` / `setInvulnerabilityTicks()`） |
| `LivingDamageEvent` | `LivingDamageEvent.Pre` / `LivingDamageEvent.Post`（Post 有 `getSource()`） |
| `LivingAttackEvent` | 已删除，最接近的是 `LivingIncomingDamageEvent`（时机从“无敌帧检查前”变成“之后”） |
| `LivingEvent.LivingTickEvent` | **`EntityTickEvent.Pre` / `EntityTickEvent.Post`** |
| `Entity#hurt(DamageSource,float):boolean` | **`Entity#hurt` 变成 `final void`；可注入的实现是 `LivingEntity#hurtServer(ServerLevel, DamageSource, float):boolean`** |
| `Entity#invulnerableTime` | 仍在 `Entity` 上（`public int`）；`LivingEntity#hurtTime/hurtDuration` 也仍是 public |
| `Level#isClientSide`（字段） | **字段私有化，改用 `Level#isClientSide()` 方法** |
| `CompoundTag#getInt/getString` | 返回 `Optional<...>`；取默认值要用 `getIntOr/getStringOr`（`putInt/putString` 不变） |
| Forge Capability + `INBTSerializable<CompoundTag>` | **`AttachmentType` / `IAttachmentHolder#getData/setData`** + `ValueIOSerializable`（`ValueOutput`/`ValueInput`） |
| `ForgeRegistries` | `BuiltInRegistries`（`ENTITY_TYPE` / `MOB_EFFECT`）；`DeferredRegister` 仍在 |
| `SimpleChannel` / `NetworkRegistry` | `RegisterPayloadHandlersEvent` + `PayloadRegistrar` + `CustomPacketPayload` + `IPayloadContext` |
| `AddReloadListenerEvent#addListener(listener)` | **`AddServerReloadListenersEvent#addListener(Identifier key, PreparableReloadListener)`**（key 必填；另有客户端版本） |
| `Event.Result.DENY` | **`MobEffectEvent.Applicable.Result.DO_NOT_APPLY`**（该事件自带的 Result 枚举：APPLY/DEFAULT/DO_NOT_APPLY） |
| `Event#isCancelable()` | 已删除；可取消事件实现 `ICancellableEvent`（`setCanceled/isCanceled`） |
| `MobEffectInstance(MobEffect, ...)` | **`MobEffectInstance(Holder<MobEffect>, ...)`**；`MobEffectInstance#getEffect()` 也返回 `Holder<MobEffect>` |
| `DustParticleOptions(Vector3f, float)` | **`DustParticleOptions(int rgb24, float)`** |
| `FMLEnvironment.dist.isClient()` | `FMLEnvironment.getDist() != Dist.CLIENT` |
| `ForgeConfigSpec` | `ModConfigSpec` |
| `META-INF/mods.toml` | `src/main/templates/META-INF/neoforge.mods.toml`（由 `generateModMetadata` 展开） |
| 清单属性 `MixinConfigs` | **`neoforge.mods.toml` 里的 `[[mixins]] config = "..."`**（与 NeoForge 自身写法一致） |
| GeckoLib 4.x | **GeckoLib 5.5.2**（大版本 API 变更，本工程未用到） |

---

# 移植记录

逐文件列出所有非平凡的改动。未列出的文件只做了机械替换
（`ResourceLocation`→`Identifier`、`ForgeRegistries.X`→`BuiltInRegistries.*`、
`.isClientSide`→`.isClientSide()`），行为不变。

## 1. 入口与注册

### `Yawning_neko_api.java`
- `@Mod` 构造器由「无参 + `FMLJavaModLoadingContext.get().getModEventBus()`」改为注入
  `(IEventBus modEventBus, ModContainer modContainer)`。
- `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`。
- **删除 `FMLCommonSetupEvent#commonSetup`**：它原本只做 `ModNetwork.register()`；
  26.1.2 的负载注册事件 `RegisterPayloadHandlersEvent` 本身就是模组总线事件，
  改成构造器里 `modEventBus.addListener(ModNetwork::register)`，没有功能损失。
- 新增 `CapabilityEventHandler.register(modEventBus)`（数据附件必须在模组总线注册）。
- `onAddReloadListeners(AddReloadListenerEvent)` → `onAddReloadListeners(AddServerReloadListenersEvent)`，
  4 个监听器各自带唯一 `Identifier` key（原 API 不需要 key）。

### `events/CapabilityEventHandler.java`（Capability → 数据附件）
- 原实现是 `@EventBusSubscriber` + `AttachCapabilitiesEvent<Entity>`，给每个 `LivingEntity`
  挂 `AdaptationDataCapability.Provider`。
- 26.1.2 数据附件**不需要逐实体附加事件**：改为一个注册持有类，
  `DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID)` 注册
  `AttachmentType<IAdaptationData> ADAPTATION_DATA`（名字沿用 `adaptation_data`），
  用 `AttachmentType.serializable(AdaptationDataCapability::new)` 获得自动持久化。
- 附件类型的可见性/读取统一走 `IAttachmentHolder#getData`，注册在模组总线（`Yawning_neko_api` 构造器调用）。

## 2. 数据附件本体

### `data/IAdaptationData.java`
- `extends INBTSerializable<CompoundTag>` → **`extends ValueIOSerializable`**
  （`serialize(ValueOutput)` / `deserialize(ValueInput)`）。
  26.1.2 已删除 `INBTSerializable`，也没有 `Capability`，所以接口里的
  `Capability<IAdaptationData> CAPABILITY` 字段被删掉，改为引用
  `CapabilityEventHandler.ADAPTATION_DATA`。其余 20 个方法签名**一字未改**。

### `data/AdaptationDataCapability.java`
- 字段与全部取值/设值逻辑保持不变。
- 根内部的 `Provider implements ICapabilitySerializable<CompoundTag>` 被删除（附件模型下不存在这个概念）。
- `serializeNBT/deserializeNBT` → `serialize(ValueOutput)/deserialize(ValueInput)`，
  **存档字段名与结构完全保持**：`adaptations` / `deathCount` / `configId` / `brokenEnd` /
  `maxAdaptations` / `lastDamageType`。
  其中动态的“伤害类型键 → 层数”映射在原版里是复合标签，ValueIO 没有 key 枚举能力，
  因此用 `Codec.unboundedMap(Codec.STRING, Codec.INT)` 表达同一份数据。
- 保留了原实现的怪癖：`lastDamageType` 会被写出但**不从存档恢复**（原 `deserializeNBT` 就是这样）。
- 保留 `spawnTime/lastHurtTime` 在反序列化时重置为 0 的行为。

## 3. 伤害管线

### `data/DamageAdaptation.java`
- `LivingHurtEvent` → **`LivingIncomingDamageEvent`**：`getAmount()/setAmount()/getSource()/setCanceled()`
  语义一一对应（原 `onLivingHurt` 方法名保留）。
- `MinecraftForge.EVENT_BUS.post(...)` → `NeoForge.EVENT_BUS.post(...)`。
- `@Mod.EventBusSubscriber(bus = FORGE)` → `@EventBusSubscriber(modid = ...)`。
- 所有 `entity.getCapability(IAdaptationData.CAPABILITY).orElse(null)` →
  `entity.getData(CapabilityEventHandler.ADAPTATION_DATA)`
  （附件缺失时返回默认值而不是 null，因此几处 `if (data == null)` 变得恒假，但为了贴近原代码保留了这些判断）。
- `registryAccess().registryOrThrow(...)` → `lookupOrThrow(...)`；
  `registry.getHolder(ResourceKey)`（已不存在）→ `registry.get(Identifier)` 拿 `Holder.Reference`，
  标签判定逻辑不变。
- 删除 `attachCapability(AttachCapabilitiesEvent)` 方法（见上）。
- 粒子颜色：`new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.2F), 1.0F)` →
  `new DustParticleOptions(0x33FF33, 1.0F)`（26.1.2 改用 RGB24 整数，颜色等价）。
- `onAddReloadListeners(AddReloadListenerEvent)` → `AddServerReloadListenersEvent` + 3 个 key。

### `events/MinimumDamageEventHandler.java`
- `LivingAttackEvent` 在 26.1.2 已删除，改用 **`LivingIncomingDamageEvent`**（`EventPriority.HIGH` 保留）。
- **行为差异（无法消除）**：`LivingAttackEvent` 在无敌帧检查之前触发，`LivingIncomingDamageEvent`
  在之后。也就是说“最低伤害”现在会被无敌帧/无敌状态拦住，不再能突破。
  直接改血量、`hurtDuration/hurtTime/invulnerableTime = 10`、`die(source)` 的逻辑原样保留。

### `damages/ModDamageTypes.java`
- 只把 `ResourceLocation` 换成 `Identifier`。**伤害类型依旧是数据包驱动的注册表**，
  由 `src/main/resources/data/yawning_neko_api/damage_type/minimum.json` 提供
  （26.1.2 该 JSON 的字段仍是 `message_id` / `exhaustion` / `scaling`，与 1.20.1 相同，**未改动**）。
  因此**不需要** `DatapackBuiltinEntriesProvider` / `RegistrySetBuilder`：那是“用代码生成数据包 JSON”的
  数据生成器手段，而本工程已经把 JSON 直接放在 `src/main/resources` 里，等价且更简单。
- **新增两个数据包标签**（用来恢复原 Mixin 的“无视无敌帧”语义，见 `mixin/LivingEntityMixin.java`）：
  - `data/minecraft/tags/damage_type/bypasses_invulnerability.json`
  - `data/minecraft/tags/damage_type/bypasses_cooldown.json`
  两者都只声明 `yawning_neko_api:minimum`。

### `mixin/LivingEntityMixin.java`
- 26.1.2 里 `Entity#hurt(DamageSource, float)` 已经是 **`final void`**，只转发到
  `hurtServer(...)`；真正实现的入口是 **`LivingEntity#hurtServer(ServerLevel, DamageSource, float): boolean`**
  （用 `javap` 对着 `minecraft-patched-26.1.2.76.jar` 核对过）。
  注入目标由 `hurt` 改为 **`hurtServer`**，回调签名相应变成
  `(ServerLevel, DamageSource, float, CallbackInfoReturnable<Boolean>)`，逻辑（创造/旁观/已死亡直接返回 false、
  直接扣血、写无敌帧、血量归零 `die`）完全保留。
- 因为 `hurtServer` 在 `isInvulnerableTo` 之后执行（原 `hurt` HEAD 注入在之前），
  “无视无敌帧”改由上面的两个伤害类型标签实现，行为与原版一致。

## 4. 网络

### `network/ModNetwork.java`
- `NetworkRegistry.newSimpleChannel` + `SimpleChannel#registerMessage` →
  `RegisterPayloadHandlersEvent#registrar(PROTOCOL_VERSION)` + `PayloadRegistrar#playToClient`。
  协议版本号仍为 `"1"`。
- `PacketDistributor.TRACKING_ENTITY.with(() -> entity)` →
  `PacketDistributor.sendToPlayersTrackingEntity(entity, payload)`。
- 原 `public static final SimpleChannel INSTANCE` 字段随 `SimpleChannel` 一起删除（该类已不存在）。

### `network/packet/AdaptationDataSyncPacket.java`、`AdaptationEventSyncPacket.java`
- 从 SimpleChannel 消息类改写为 `CustomPacketPayload`：新增 `TYPE`
  （`yawning_neko_api:adaptation_data` / `yawning_neko_api:adaptation_event`）与
  `STREAM_CODEC`（`CustomPacketPayload.codec(::encode, ::new)`），`handle` 由
  `Supplier<NetworkEvent.Context>` 改成 `IPayloadContext`。
- 缓冲区类型 `FriendlyByteBuf` → `RegistryFriendlyByteBuf`（`CustomPacketPayload` 的编解码器要求）。
- **同步的字段与写入顺序完全不变**（`int entityId` / `int adaptationLevel` / `utf damageTypeKeyStr` /
  `int maxAdaptations`，以及 `int entityId` / `int eventType`），所以线上数据格式一致。
- 取实体：`ctx.get().getSender().level().getEntity(entityId)` → `ctx.player().level().getEntity(entityId)`。

## 5. 事件与注册表杂项

### `events/AdaptationEffectEvent.java`
- 删除了 `@Override public boolean isCancelable()`（26.1.2 的 `Event` 没有这个方法，
  可取消性改由 `ICancellableEvent` 标记）。事件本身依旧不可取消，构造器/字段/getter/枚举不变。

### `events/EffectImmunityHandler.java`
- `event.setResult(Event.Result.DENY)` → `event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY)`。
- `ForgeRegistries.MOB_EFFECTS.getKey(effectInstance.getEffect())` 不能再用：
  `getEffect()` 现在返回 `Holder<MobEffect>`，改为
  `BuiltInRegistries.MOB_EFFECT.getKey(effectInstance.getEffect().value())`，并加了 null 保护。

### `events/EntityAttackEffectHandler.java`
- `LivingDamageEvent` → `LivingDamageEvent.Post`。
- 施加效果时 `new MobEffectInstance(MobEffect, ...)` → `new MobEffectInstance(Holder<MobEffect>, ...)`，
  因此改为 `BuiltInRegistries.MOB_EFFECT.get(id)` 取 `Holder`（并用 `Identifier.tryParse` 做保护）。

### `data/DamageAdaptationManager.java`
- `FMLEnvironment.dist.isClient()` → `FMLEnvironment.getDist() != Dist.CLIENT`。

### `data/EntityUpgradeManager.java`
- `getPersistentData().getInt("AttackUpgradeLevel")` →
  `getPersistentData().getIntOr("AttackUpgradeLevel", 0)`（26.1.2 的 `getInt` 返回 `Optional<Integer>`）。
  写入侧 `putInt` 不变，NBT 键名不变。

## 6. 资源

### `src/main/resources/yawning_neko_api.mixins.json`
- 删掉 `"compatibilityLevel": "JAVA_8"` 和 `"refmap": "yawning_neko_api.refmap.json"`，
  删掉 `"overwrites": {"requireAnnotations": true}`，其余（`required` / `minVersion` / `package` /
  `mixins` / `client` / `injectors`）保持不变。
  这正是 NeoForge 26.1.2 自带 `neoforge.mixins.json` 的字段集合：NeoForge 用官方命名，不再需要 refmap，
  compatibilityLevel 也交给 Mixin 自己按运行期 JVM 推断。

### `src/main/templates/META-INF/neoforge.mods.toml`
- 新增 `loaderVersion = "[3,]"` 与
  ```toml
  [[mixins]]
  config = "yawning_neko_api.mixins.json"
  ```
  26.1.2 的 FML 从 mods.toml 的 `[[mixins]]` 读取 Mixin 配置（已从 loader 的
  `ModFileParser` 字符串与 NeoForge 自带的 `META-INF/neoforge.mods.toml` 双向确认），
  ModDevGradle 不会自动往清单里写 `MixinConfigs`，所以这一条是必需的。

### `src/main/resources/pack.mcmeta`
- `"pack_format": 15` 在 26.1.2 会直接报错：超过 64 的版本号必须写成
  `min_format`/`max_format`（`PackFormat.IntermediaryFormat#validate`）。
  改成同时覆盖客户端资源格式（84.0）与数据包格式（101.1）：
  ```json
  { "pack": { "description": "yawning_neko_api resources", "min_format": 84, "max_format": [101, 1] } }
  ```
  （数值取自 26.1.2 的 `version.json`：`resource_major=84`、`data_major=101`、`data_minor=1`。）

### 未改动
- `data/yawning_neko_api/damage_type/minimum.json`（字段格式与 1.20.1 相同）
- `yawning_neko_api_icon.png`、`META-INF/mods.toml.1.20.1`（后者被 `build.gradle` 排除打包）

## 7. 公共 API 上被迫的取舍（供调用方参考）

| 原名 | 现状 |
|---|---|
| `IAdaptationData.CAPABILITY` | 删除；等价物是 `CapabilityEventHandler.ADAPTATION_DATA`（`AttachmentType<IAdaptationData>`） |
| `IAdaptationData#serializeNBT()/deserializeNBT(CompoundTag)` | 改为 `serialize(ValueOutput)/deserialize(ValueInput)`（`ValueIOSerializable`） |
| `AdaptationDataCapability.Provider` | 删除，附件模型下无对应概念 |
| `DamageAdaptation.attachCapability(AttachCapabilitiesEvent)` | 删除，附件不再需要逐实体附加 |
| `ModNetwork.INSTANCE` (`SimpleChannel`) | 删除，改用 `ModNetwork.register(RegisterPayloadHandlersEvent)` |
| `MinimumDamageEventHandler.onLivingAttack(LivingAttackEvent)` | 参数类型改为 `LivingIncomingDamageEvent`（方法名保留） |

其余类名、方法名、字段名、枚举名、JSON 资源路径**均与 1.20.1 版本一致**。
