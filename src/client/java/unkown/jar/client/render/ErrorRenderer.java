package unkown.jar.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import unkown.jar.UnkownJar;
import unkown.jar.entity.ErrorEntity;
import unkown.jar.entity.ModEntities;

/** Renderer, render state and model of ERROR (the script monster), all in one file. */
public class ErrorRenderer extends HumanoidMobRenderer<ErrorEntity, ErrorRenderState, ErrorModel> {
	private static final Identifier TEXTURE = UnkownJar.id("textures/entity/error.png");

	/** Called from the client initializer: registers the model layer and the renderer. */
	public static void init() {
		EntityModelLayerRegistry.registerModelLayer(ErrorModel.LAYER, ErrorModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.ERROR, ErrorRenderer::new);
	}

	public ErrorRenderer(EntityRendererProvider.Context context) {
		super(context, new ErrorModel(context.bakeLayer(ErrorModel.LAYER)), 0.8F);
	}

	@Override
	public ErrorRenderState createRenderState() {
		return new ErrorRenderState();
	}

	@Override
	public void extractRenderState(ErrorEntity entity, ErrorRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.attackTicks = entity.getAttackTicks();
		state.chasing = entity.isChasing();
	}

	@Override
	public Identifier getTextureLocation(ErrorRenderState state) {
		return TEXTURE;
	}
}

class ErrorRenderState extends HumanoidRenderState {
	public int attackTicks;
	public boolean chasing;
}

/**
 * ERROR: very tall and thin, a monitor for a head (one big eye, huge teeth), hanging cables,
 * extremely long arms with four-fingered claws, long legs with claw feet, orbiting code windows and shards.
 */
class ErrorModel extends HumanoidModel<ErrorRenderState> {
	static final ModelLayerLocation LAYER = new ModelLayerLocation(UnkownJar.id("error"), "main");

	private static final int CABLES = 6;
	private static final int PANELS = 4;
	private static final int SHARDS = 6;
	private static final int ATTACK_DURATION = 14;

	private final ModelPart[] foreArm = new ModelPart[2];   // 0 = right, 1 = left
	private final ModelPart[][] fingers = new ModelPart[2][4];
	private final ModelPart[][] tips = new ModelPart[2][4];
	private final ModelPart[] shin = new ModelPart[2];
	private final ModelPart[] cables = new ModelPart[CABLES];
	private final ModelPart[] panels = new ModelPart[PANELS];
	private final ModelPart[] shards = new ModelPart[SHARDS];

	ErrorModel(ModelPart root) {
		super(root);
		String[] arms = {"right_arm", "left_arm"};
		String[] legs = {"right_leg", "left_leg"};
		for (int s = 0; s < 2; s++) {
			ModelPart fore = root.getChild(arms[s]).getChild("fore");
			this.foreArm[s] = fore;
			for (int i = 0; i < 4; i++) {
				ModelPart f = fore.getChild("f" + i);
				this.fingers[s][i] = f;
				this.tips[s][i] = f.getChild("tip");
			}
			this.shin[s] = root.getChild(legs[s]).getChild("shin");
		}
		ModelPart head = root.getChild("head");
		for (int i = 0; i < CABLES; i++) {
			this.cables[i] = head.getChild("cab" + i);
		}
		for (int i = 0; i < PANELS; i++) {
			this.panels[i] = root.getChild("panel" + i);
		}
		for (int i = 0; i < SHARDS; i++) {
			this.shards[i] = root.getChild("shard" + i);
		}
	}

	static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// monitor head with cables
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -14.0F, -3.5F, 16.0F, 13.0F, 7.0F),
				PartPose.offset(0.0F, -14.0F, 0.0F));
		for (int i = 0; i < CABLES; i++) {
			head.addOrReplaceChild("cab" + i,
					CubeListBuilder.create().texOffs(64, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 14.0F, 1.0F),
					PartPose.offset(-6.0F + i * 2.4F, -1.0F, i % 2 == 0 ? -1.5F : 1.5F));
		}
		root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, -14.0F, 0.0F));

		// thin torso
		root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(64, 0).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 16.0F, 3.0F),
				PartPose.offset(0.0F, -14.0F, 0.0F));

		// very long arms with forearm and four claws each
		for (int s = 0; s < 2; s++) {
			float x = s == 0 ? -5.0F : 5.0F;
			PartDefinition arm = root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 16.0F, 2.0F),
					PartPose.offset(x, -12.0F, 0.0F));
			PartDefinition fore = arm.addOrReplaceChild("fore",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
					PartPose.offset(0.0F, 15.0F, 0.0F));
			for (int i = 0; i < 4; i++) {
				PartDefinition finger = fore.addOrReplaceChild("f" + i,
						CubeListBuilder.create().texOffs(64, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
						PartPose.offsetAndRotation(-1.2F + i * 0.8F, 16.0F, 0.0F, 0.2F, 0.0F, (i - 1.5F) * 0.2F));
				finger.addOrReplaceChild("tip",
						CubeListBuilder.create().texOffs(64, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
						PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.9F, 0.0F, 0.0F));
			}
		}

		// long legs: thigh + shin + three claw toes
		for (int s = 0; s < 2; s++) {
			float x = s == 0 ? -2.5F : 2.5F;
			PartDefinition leg = root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
					PartPose.offset(x, 2.0F, 0.0F));
			PartDefinition sh = leg.addOrReplaceChild("shin",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
					PartPose.offset(0.0F, 11.0F, 0.0F));
			for (int i = 0; i < 3; i++) {
				sh.addOrReplaceChild("toe" + i,
						CubeListBuilder.create().texOffs(64, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 4.0F, 1.0F),
						PartPose.offsetAndRotation(-0.9F + i * 0.9F, 10.5F, -0.5F, -1.4F, 0.0F, 0.0F));
			}
		}

		// floating code windows (each has its own texture area) and shards
		for (int i = 0; i < PANELS; i++) {
			root.addOrReplaceChild("panel" + i,
					CubeListBuilder.create().texOffs(0, 24 + 10 * i).addBox(-6.0F, -4.0F, -0.5F, 12.0F, 8.0F, 1.0F),
					PartPose.ZERO);
		}
		for (int i = 0; i < SHARDS; i++) {
			root.addOrReplaceChild("shard" + i,
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
					PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(ErrorRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		boolean run = state.chasing;
		float walk = state.walkAnimationPos;
		float spd = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);

		// ---- idle / glitchy twitching
		this.head.zRot = 0.04F * Mth.sin(t * 7.0F);
		int glitch = (int) t % 53;
		if (glitch < 3) {
			this.head.zRot = (glitch % 2 == 0 ? 0.45F : -0.4F);
			this.head.yRot += (glitch % 2 == 0 ? 0.5F : -0.6F);
		}
		this.rightArm.xRot = 0.05F + 0.05F * Mth.sin(t * 0.07F);
		this.leftArm.xRot = 0.05F + 0.05F * Mth.sin(t * 0.07F + 2.0F);
		this.rightArm.zRot = 0.08F;
		this.leftArm.zRot = -0.08F;
		this.foreArm[0].xRot = -0.12F;
		this.foreArm[1].xRot = -0.12F;
		this.body.xRot = 0.04F * Mth.sin(t * 0.05F);

		// ---- walk and RUN animation
		float swing = Mth.cos(walk * 0.6662F);
		float swing2 = Mth.cos(walk * 0.6662F + (float) Math.PI);
		if (run) {
			// sprint: long strides, deep knee bend, body thrown forward, arms swept back, head low
			float k = 0.6F + spd;
			this.rightLeg.xRot = swing * 1.5F * k;
			this.leftLeg.xRot = swing2 * 1.5F * k;
			this.shin[0].xRot = 0.3F + Math.max(0.0F, -this.rightLeg.xRot) * 1.5F;
			this.shin[1].xRot = 0.3F + Math.max(0.0F, -this.leftLeg.xRot) * 1.5F;
			this.body.xRot = 0.55F;
			this.head.xRot += 0.35F;
			this.rightArm.xRot = 0.9F + swing2 * 1.0F * k;
			this.leftArm.xRot = 0.9F + swing * 1.0F * k;
			this.foreArm[0].xRot = -0.5F - Math.max(0.0F, swing2) * 0.6F;
			this.foreArm[1].xRot = -0.5F - Math.max(0.0F, swing) * 0.6F;
		} else {
			this.shin[0].xRot = 0.1F + Math.max(0.0F, -this.rightLeg.xRot) * 1.2F;
			this.shin[1].xRot = 0.1F + Math.max(0.0F, -this.leftLeg.xRot) * 1.2F;
		}

		// ---- ATTACK animation: claws up and back, then a heavy downward slash
		float spread = 1.0F;
		if (state.attackTicks > 0) {
			float p = 1.0F - state.attackTicks / (float) ATTACK_DURATION;
			if (p < 0.45F) {
				float k = p / 0.45F;
				this.rightArm.xRot = -2.7F * k;
				this.leftArm.xRot = -2.3F * k;
				this.foreArm[0].xRot = -0.8F * k;
				this.foreArm[1].xRot = -0.8F * k;
				this.body.xRot = -0.25F * k;
				this.head.xRot -= 0.3F * k;
				spread = 1.0F + 0.8F * k;
			} else {
				float k = (p - 0.45F) / 0.55F;
				float e = 1.0F - (1.0F - k) * (1.0F - k);
				this.rightArm.xRot = -2.7F + 3.3F * e;
				this.leftArm.xRot = -2.3F + 3.0F * e;
				this.foreArm[0].xRot = -0.8F + 1.0F * e;
				this.foreArm[1].xRot = -0.8F + 1.0F * e;
				this.body.xRot = -0.25F + 0.95F * e;
				this.head.xRot += 0.5F * e;
				spread = 1.8F;
			}
			this.rightArm.zRot = 0.25F;
			this.leftArm.zRot = -0.25F;
		}

		// ---- claws: always twitching, spread wide when attacking
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i < 4; i++) {
				float tw = Mth.sin(t * 0.35F + i * 1.3F + s * 2.0F);
				this.fingers[s][i].zRot = (i - 1.5F) * 0.2F * spread + 0.05F * tw;
				this.fingers[s][i].xRot = 0.2F - 0.15F * spread + 0.12F * tw;
				this.tips[s][i].xRot = 0.9F + 0.25F * tw + (state.attackTicks > 0 ? 0.3F : 0.0F);
			}
		}

		// ---- cables sway, or stream backwards while running
		for (int i = 0; i < CABLES; i++) {
			float ph = i * 1.4F;
			this.cables[i].xRot = (run ? 0.9F : 0.1F) + 0.15F * Mth.sin(t * (run ? 0.5F : 0.18F) + ph);
			this.cables[i].zRot = 0.18F * Mth.sin(t * 0.15F + ph * 1.3F);
		}

		// ---- orbiting code windows and shards (flicker in and out)
		float orbit = t * (run ? 0.07F : 0.03F);
		for (int i = 0; i < PANELS; i++) {
			float a = orbit + i * ((float) Math.PI / 2.0F);
			float r = 19.0F + 2.0F * Mth.sin(t * 0.04F + i);
			this.panels[i].x = Mth.cos(a) * r;
			this.panels[i].z = Mth.sin(a) * r;
			this.panels[i].y = -14.0F + 7.0F * Mth.sin(t * 0.05F + i * 1.7F);
			this.panels[i].yRot = -a - (float) Math.PI / 2.0F;
			this.panels[i].zRot = 0.12F * Mth.sin(t * 0.09F + i);
			this.panels[i].visible = ((int) (t + i * 9) % 47) > 3;
		}
		for (int i = 0; i < SHARDS; i++) {
			float a = -orbit * 1.6F + i * 1.05F;
			float r = 13.0F + 6.0F * Mth.sin(t * 0.06F + i * 2.1F);
			this.shards[i].x = Mth.cos(a) * r;
			this.shards[i].z = Mth.sin(a) * r;
			this.shards[i].y = -24.0F + 20.0F * Mth.sin(t * 0.045F + i * 1.9F);
			this.shards[i].xRot = t * 0.1F + i;
			this.shards[i].yRot = t * 0.13F + i * 2.0F;
			this.shards[i].visible = ((int) (t + i * 5) % 31) > 2;
		}
	}
}
