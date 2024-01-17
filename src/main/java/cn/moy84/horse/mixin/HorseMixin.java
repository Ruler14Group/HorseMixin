package cn.moy84.horse.mixin;

import cn.moy84.horse.interfaces.HorseMixinInterface;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HorseBaseEntity.class)
public abstract class HorseMixin extends AnimalEntity implements HorseMixinInterface, InventoryChangedListener, JumpingMount, Saddleable{

    @Shadow protected int soundTicks;
    @Shadow protected float jumpStrength;
    @Shadow public abstract boolean isAngry();
    @Shadow public abstract boolean isInAir();
    @Shadow public abstract double getJumpStrength();
    @Shadow public abstract void setInAir(boolean inAir);

    @Unique private int valueThirst = 10;
    @Unique private int valueTired = 100;
    @Unique private int thirstTimeCount = 0;
    @Unique private int runTimeCount = 0;

    @Override
    public int getValueThirst() {
        return valueThirst;
    }

    public void setValueThirst(int valueThirst) {
        this.valueThirst = valueThirst;
    }

    @Unique private int restTimeCount = 0;
    @Unique private int sprintTimeCount = 0;
    @Unique private float tempSpeed = 0;

    protected HorseMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "writeCustomDataToNbt", at=@At("RETURN"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("thirst", valueThirst);
        nbt.putInt("tired", valueTired);
    }

    @Inject(method = "readCustomDataFromNbt", at=@At("RETURN"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("thirst")){
            valueThirst = nbt.getInt("thirst");
            valueTired = nbt.getInt("tired");
        }
    }

    @Inject(method = "startJumping", at=@At("HEAD"), cancellable = true)
    public void startJumping(int height, CallbackInfo ci){
        if (valueTired < 5){
            ci.cancel();
        }
        valueTired-=5;
    }

    @Inject(method = "receiveFood", at=@At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void receiveFood(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir, boolean bl) {
        if (bl){
            onFeed();
        }
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isAlive()) {
            if (!this.hasPassengers()){
                onRest();
                sprintTimeCount++;
            }
            if (this.hasPassengers() && this.canBeControlledByRider() && this.isSaddled()) {
                LivingEntity livingEntity = (LivingEntity)this.getPrimaryPassenger();
                updateData(this, livingEntity);
                this.setYaw(livingEntity.getYaw());
                this.prevYaw = this.getYaw();
                this.setPitch(livingEntity.getPitch() * 0.5F);
                this.setRotation(this.getYaw(), this.getPitch());
                this.bodyYaw = this.getYaw();
                this.headYaw = this.bodyYaw;
                float f = livingEntity.sidewaysSpeed * 0.5F;
                float g = livingEntity.forwardSpeed;
                if (g <= 0.0F) {
                    g *= 0.25F;
                    this.soundTicks = 0;
                }
                onRidden();
                sprintTimeCount++;
                if (horizontalSpeed>tempSpeed){
                    tempSpeed = horizontalSpeed;
                    onRun();
                }else {
                    onRest();
                }

                if (this.onGround && this.jumpStrength == 0.0F && this.isAngry() && !this.jumping) {
                    f = 0.0F;
                    g = 0.0F;
                }

                if (this.jumpStrength > 0.0F && !this.isInAir() && this.onGround) {
                    double d = this.getJumpStrength() * (double)this.jumpStrength * (double)this.getJumpVelocityMultiplier();
                    double e = d + this.getJumpBoostVelocityModifier();
                    Vec3d vec3d = this.getVelocity();
                    this.setVelocity(vec3d.x, e, vec3d.z);
                    this.setInAir(true);
                    this.velocityDirty = true;
                    if (g > 0.0F) {
                        float h = MathHelper.sin(this.getYaw() * 0.017453292F);
                        float i = MathHelper.cos(this.getYaw() * 0.017453292F);
                        this.setVelocity(this.getVelocity().add((double)(-0.4F * h * this.jumpStrength), 0.0, (double)(0.4F * i * this.jumpStrength)));
                    }

                    this.jumpStrength = 0.0F;
                }

                this.airStrafingSpeed = this.getMovementSpeed() * 0.1F;
                if (this.isLogicalSideForUpdatingMovement()) {
                    this.setMovementSpeed((float)this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
                    super.travel(new Vec3d((double)f, movementInput.y, (double)g));
                } else if (livingEntity instanceof PlayerEntity) {
                    this.setVelocity(Vec3d.ZERO);
                }

                if (this.onGround) {
                    this.jumpStrength = 0.0F;
                    this.setInAir(false);
                }

                this.updateLimbs(this, false);
                this.tryCheckBlockCollision();
            } else {
                this.airStrafingSpeed = 0.02F;
                super.travel(movementInput);
            }
        }
    }

    public void onRidden(){
        thirstTimeCount++;
        if (thirstTimeCount >= 20*60) {
            thirstTimeCount = 0;
            valueThirst -= 1;
        }
    }

    public void onRun(){
        runTimeCount++;
        if (runTimeCount >= 20) {
            runTimeCount = 0;
            valueTired -= 2;
        }
    }

    public void onSprint(){
        if (valueTired>10 && sprintTimeCount>=200){
            addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 2));
            sprintTimeCount = 0;
            valueTired-=10;
        };
    }

    public void onFeed(){
        valueTired += 10;
    }

    public void onRest(){
        restTimeCount++;
        if (valueTired<10){
            if (restTimeCount >= 200){
                restTimeCount = 0;
                valueTired += 2;
            }
        }else if (restTimeCount >= 20) {
            restTimeCount = 0;
            valueTired += 2;
        }
    }

    public void updateData(AnimalEntity animalEntity,LivingEntity livingEntity){
        if (valueTired > 100){
            valueTired = 100;
        }
        if (valueTired < 0){
            valueTired = 0;
        }
        if (valueThirst > 10){
            valueThirst = 10;
        }
        if (valueThirst < 0){
            valueThirst = 0;
        }
        if (sprintTimeCount >200){
            sprintTimeCount = 200;
        }

        if (valueThirst<3){
            animalEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1, 10));
        }
        if (valueTired <= 5){
            animalEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1, 10));
        }else if(valueTired <= 10){
            animalEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1, 3));
        }

        if (livingEntity instanceof ServerPlayerEntity){
            ServerPlayerEntity player = (ServerPlayerEntity) livingEntity;
            player.sendMessage(Text.of("谁家小马！疲劳值："+valueTired+"%，饮水值："+valueThirst+"，冲刺冷却！："+ (10 -sprintTimeCount/20)), true);
        }

    }
}
