/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.mojo;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.ihtsdo.otf.tcc.api.blueprint.DescriptionCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RelationshipCAB;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.ihtsdo.otf.tcc.api.lang.LanguageCode;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.ihtsdo.otf.tcc.api.refex.RefexType;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpecWithDescriptions;
import org.ihtsdo.otf.tcc.api.spec.RelSpec;
import org.ihtsdo.otf.tcc.dto.TtkConceptChronicle;
import org.ihtsdo.otf.tcc.dto.component.TtkComponentChronicle;
import org.ihtsdo.otf.tcc.dto.component.TtkRevision;
import org.ihtsdo.otf.tcc.dto.component.attribute.TtkConceptAttributesChronicle;
import org.ihtsdo.otf.tcc.dto.component.description.TtkDescriptionChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.TtkRefexAbstractMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.type_uuid.TtkRefexUuidMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.relationship.TtkRelationshipChronicle;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.util.UuidT5Generator;

/**
 * {@link GenerateMetadataEConcepts}
 * 
 * A utility class to pick up {@link ConceptSpec} entries, and write them out to an eConcept file.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */



@Deprecated  //THIS class can go away... however, leaving the code for now, as some of it still needs to migrate elsewhere.
public class GenerateMetadataEConcepts extends AbstractMojo
{
	private static final UUID authorUuid_ = IsaacMetadataAuxiliaryBinding.USER.getPrimodialUuid();
	private static final UUID pathUUID_ = IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid();
	private static final UUID moduleUuid_ = IsaacMetadataAuxiliaryBinding.ISAAC_MODULE.getPrimodialUuid();
	private static final LanguageCode lang_ = LanguageCode.EN;
	private static final UUID isARelUuid_ = IsaacMetadataAuxiliaryBinding.IS_A.getPrimodialUuid();
	private static final UUID definingCharacteristicStatedUuid_ =  IsaacMetadataAuxiliaryBinding.STATED.getPrimodialUuid();
	private static final UUID notRefinableUuid = SnomedMetadataRf2.NOT_REFINABLE_RF2.getPrimodialUuid();  //TODO OCHRE need metadata for this?
	private static final UUID descriptionAcceptableUuid_ = IsaacMetadataAuxiliaryBinding.ACCEPTABLE.getPrimodialUuid();
	private static final UUID descriptionPreferredUuid_ = IsaacMetadataAuxiliaryBinding.PREFERRED.getPrimodialUuid();
	private static final UUID usEnRefsetUuid_ = IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT.getPrimodialUuid();
	private static final UUID refsetMemberTypeNormalMemberUuid_ = IsaacMetadataAuxiliaryBinding.NORMAL_MEMBER.getPrimodialUuid();

	private static enum DescriptionType
	{
		FSN, SYNONYM, DEFINITION;

		public UUID getTypeUUID()
		{
			if (this == FSN)
			{
				return IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getPrimodialUuid();
			}
			else if (this == SYNONYM)
			{
				return IsaacMetadataAuxiliaryBinding.SYNONYM.getPrimodialUuid();
			}
			else if (this == DEFINITION)
			{
				return IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getPrimodialUuid();
			}
			throw new RuntimeException("impossible");
		}
	}

	/**
	 * Name and location of the output file.
	 */
	@Parameter (required = true)
	private File outputFile;

	/**
	 * Fully specified class names which should be scanned for public, static variables which are instances of {@link ConceptSpec}.
	 * 
	 * Each {@link ConceptSpec} found will be output to the eConcept file.
	 */
	@Parameter
	private String[] classesWithConceptSpecs;

	/**
	 * Any other {@link ConceptSpec} which should be built into the eConcept file.
	 */
	@Parameter
	private ConceptSpec[] conceptSpecs;
	
	/**
	 * UUIDs of root concepts from the terminologies that are being loaded which should be added
	 * as children of {@link ISAAC#ISAAC_ROOT}.  Eventually specifying these should be unnecessary, 
	 * as eConcepts file should link their own root concepts to the new isaac root
	 */
	
	@Parameter
	private MojoConceptSpec[] terminologyRoots;

	/**
	 * Instead of writing the default jbin format, write the eccs change set format instead.
	 */
	@Parameter
	private boolean writeAsChangeSetFormat = false;
	
	/**
	 * This constructor is just for Maven - don't use.
	 */
	public GenerateMetadataEConcepts()
	{
		
	}
	
	/**
	 * Constructor for programmatic (non maven) access
	 * @param outputFile
	 * @param classesWithConceptSpecs
	 * @param conceptSpecs
	 * @param writeAsChangeSetFormat
	 */
	public GenerateMetadataEConcepts(File outputFile, String[] classesWithConceptSpecs, ConceptSpec[] conceptSpecs, boolean writeAsChangeSetFormat)
	{
		this.outputFile = outputFile;
		this.classesWithConceptSpecs = classesWithConceptSpecs;
		this.conceptSpecs = conceptSpecs;
		this.writeAsChangeSetFormat = writeAsChangeSetFormat;
	}

	/**
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		throw new MojoExecutionException("This shouldn't be used any longer");
//		try
//		{
//			outputFile = outputFile.getAbsoluteFile();
//			outputFile.getParentFile().mkdirs();
//			if (!outputFile.getParentFile().exists())
//			{
//				throw new MojoExecutionException("Cannot create the folder " + outputFile.getParentFile().getAbsolutePath());
//			}
//
//			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
//			
//			ArrayList<ConceptSpec> conceptSpecsToProcess = new ArrayList<>();
//			
//
//			if (conceptSpecs != null)
//			{
//				for (ConceptSpec cs : conceptSpecs)
//				{
//					conceptSpecsToProcess.add(cs);
//				}
//			}
//			if (classesWithConceptSpecs != null)
//			{
//				for (String cs : classesWithConceptSpecs)
//				{
//					conceptSpecsToProcess.addAll(getSpecsFromClass(cs));
//				}
//			}
//			
//			if (terminologyRoots != null && terminologyRoots.length > 0)
//			{
//				getLog().info("Will create " + terminologyRoots.length + " additional root concepts");
//				for (int i = 0; i < terminologyRoots.length; i++)
//				{
//					ConceptSpec cs = terminologyRoots[i].getConceptSpec();
//					//Need to do stated and inferred, otherwise, we can't browse, on inferred mode, nor on inferred_then_stated mode
//					cs.setRelSpecs(new RelSpec[] {new RelSpec(cs, IsaacMetadataAuxiliaryBinding.IS_A, IsaacMetadataAuxiliaryBinding.ISAAC_ROOT)}); //stated
//					
//					//leave this off for now, see if classifier cleans it up
//							//new RelSpec(conceptsToAddAsRoots[i], Snomed.IS_A, ISAAC.ISAAC_ROOT, SnomedMetadataRf2.INFERRED_RELATIONSHIP_RF2)}); //inferred  
//					conceptSpecsToProcess.add(cs);
//				}
//			}
//			
//			List<UUID> refexesToIndex = new ArrayList<>();
//			List<Integer[]> columnsToIndex = new ArrayList<>();
//			TtkConceptChronicle indexConfigConcept = null;
//			
//			int count = 0;
//			for (ConceptSpec cs : conceptSpecsToProcess)
//			{
//				TtkConceptChronicle converted = convert(cs);
//				
//				if (IsaacMetadataConstants.DYNAMIC_SEMEME_INDEX_CONFIGURATION.getUUID().equals(cs.getPrimodialUuid()))
//				{
//					//Need to delay writing this concept
//					indexConfigConcept = converted;
//				}
//				else
//				{
//					if (writeAsChangeSetFormat)
//					{
//						dos.writeLong(System.currentTimeMillis());
//					}
//					converted.writeExternal(dos);
//					count++;
//				}
//			}
//			if (indexConfigConcept != null)
//			{
//				if (writeAsChangeSetFormat)
//				{
//					dos.writeLong(System.currentTimeMillis());
//				}
//				configureDynamicRefexIndexes(indexConfigConcept, refexesToIndex, columnsToIndex);
//				indexConfigConcept.writeExternal(dos);
//				count++;
//			}
//			dos.flush();
//			dos.close();
//			getLog().info("Wrote " + count + " concepts to " + outputFile.getAbsolutePath() + ".");
//		}
//		catch (IOException | IllegalArgumentException | IllegalAccessException | ClassNotFoundException | NoSuchAlgorithmException | PropertyVetoException e)
//		{
//			throw new MojoExecutionException("Failure", e);
//		}
	}
	
	private List<ConceptSpec> getSpecsFromClass(String className) throws IllegalArgumentException, IllegalAccessException, ClassNotFoundException
	{
		ArrayList<ConceptSpec> results = new ArrayList<>();
		
		Class<?> clazz = getClass().getClassLoader().loadClass(className);
		
		for (Field f : clazz.getDeclaredFields())
		{
			if (f.getType().equals(ConceptSpec.class) || f.getType().equals(ConceptSpecWithDescriptions.class))
			{
				f.setAccessible(true);
				results.add((ConceptSpec)f.get(null));
			}
		}
		getLog().info("Got " + results.size() + " concept specs from " + className);
		return results;
	}

	public static TtkConceptChronicle convert(ConceptSpec cs) throws IOException
	{
		TtkConceptChronicle cc = new TtkConceptChronicle();

		if (cs.getUuids().length < 1)
		{
			throw new IOException("Concept Spec " + cs + " does not have a UUID");
		}

		cc.setPrimordialUuid(cs.getPrimodialUuid());

		TtkConceptAttributesChronicle conceptAttributes = new TtkConceptAttributesChronicle();
		conceptAttributes.setDefined(false);
		conceptAttributes.setPrimordialComponentUuid(cs.getPrimodialUuid());
		setRevisionAttributes(conceptAttributes, null, System.currentTimeMillis());
		cc.setConceptAttributes(conceptAttributes);

		for (int i = 1; i < cs.getUuids().length; i++)
		{
			cc.getConceptAttributes().getUuids().add(cs.getUuids()[i]);
		}
		
		if (cs instanceof ConceptSpecWithDescriptions)
		{
			ConceptSpecWithDescriptions csd = (ConceptSpecWithDescriptions)cs;
			addDescription(cc, csd.getFsn(), DescriptionType.FSN, true);
			
			boolean first = true;
			
			for (String s : csd.getSynonyms())
			{
				addDescription(cc, s, DescriptionType.SYNONYM, first);
				first = false;
			}
			first = true;
			for (String s : csd.getDefinitions())
			{
				addDescription(cc, s, DescriptionType.DEFINITION, first);
			}
			
		}
		else
		{
			addDescription(cc, cs.getConceptDescriptionText(), DescriptionType.FSN, true);
			addDescription(cc, cs.getConceptDescriptionText(), DescriptionType.SYNONYM, true);
		}

		for (RelSpec rs : cs.getRelSpecs())
		{
			addRelationship(cc, rs.getDestinationSpec().getPrimodialUuid(), rs.getRelTypeSpec().getPrimodialUuid(), 
					(rs.getCharacteristicSpec() == null ? null : rs.getCharacteristicSpec().getPrimodialUuid()), null);
		}
		return cc;
	}

	/**
	 * Set up all the boilerplate stuff.
	 * 
	 * @param object - The object to do the setting to
	 * @param statusUuid - Uuid or null (for current)
	 * @param time - time or null (for global default value - essentially 'now')
	 */
	public static void setRevisionAttributes(TtkRevision object, Status status, long time)
	{
		object.setAuthorUuid(authorUuid_);
		object.setModuleUuid(moduleUuid_);
		object.setPathUuid(pathUUID_);
		object.setStatus(status == null ? Status.ACTIVE : status);
		object.setTime(time);
	}

	/**
	 * Add a description to the concept.
	 * 
	 * @param time - if null, set to the time on the concept.
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
	public static TtkDescriptionChronicle addDescription(TtkConceptChronicle eConcept, String descriptionValue, DescriptionType wbDescriptionType, boolean preferred)
	{
			List<TtkDescriptionChronicle> descriptions = eConcept.getDescriptions();
			if (descriptions == null)
			{
				descriptions = new ArrayList<TtkDescriptionChronicle>();
				eConcept.setDescriptions(descriptions);
			}
			TtkDescriptionChronicle description = new TtkDescriptionChronicle();
			description.setConceptUuid(eConcept.getPrimordialUuid());
			description.setLang(lang_.getFormatedLanguageNoDialectCode());
			description.setPrimordialComponentUuid(UuidT5Generator.get(DescriptionCAB.descSpecNamespace,
					eConcept.getPrimordialUuid().toString() + wbDescriptionType.getTypeUUID() + lang_.getFormatedLanguageNoDialectCode() + descriptionValue));
			description.setTypeUuid(wbDescriptionType.getTypeUUID());
			description.setText(descriptionValue);
			setRevisionAttributes(description, Status.ACTIVE, eConcept.getConceptAttributes().getTime());
			descriptions.add(description);
			addUuidAnnotation(description, (preferred ? descriptionPreferredUuid_ : descriptionAcceptableUuid_), usEnRefsetUuid_);
			return description;
	}

	/**
	 * @param time - If time is null, uses the component time.
	 * @param valueConcept - if value is null, it uses RefsetAuxiliary.Concept.NORMAL_MEMBER.getPrimoridalUid()
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 */
	public static TtkRefexUuidMemberChronicle addUuidAnnotation(TtkComponentChronicle<?,?> component, UUID valueConcept, UUID refsetUuid)
	{
			List<TtkRefexAbstractMemberChronicle<?>> annotations = component.getAnnotations();
			if (annotations == null)
			{
				annotations = new ArrayList<TtkRefexAbstractMemberChronicle<?>>();
				component.setAnnotations(annotations);
			}
			TtkRefexUuidMemberChronicle conceptRefexMember = new TtkRefexUuidMemberChronicle();
			conceptRefexMember.setReferencedComponentUuid(component.getPrimordialComponentUuid());
			conceptRefexMember.setPrimordialComponentUuid(UuidT5Generator.get(RefexCAB.refexSpecNamespace, RefexType.MEMBER.name() + refsetUuid.toString()
					+ component.getPrimordialComponentUuid().toString()));
			conceptRefexMember.setUuid1(valueConcept == null ? refsetMemberTypeNormalMemberUuid_ : valueConcept);
			conceptRefexMember.setAssemblageUuid(refsetUuid);
			setRevisionAttributes(conceptRefexMember, Status.ACTIVE, component.getTime());
			annotations.add(conceptRefexMember);
			return conceptRefexMember;
	}
	
	/**
	 * Add a relationship. The source of the relationship is assumed to be the specified concept.
	 * 
	 * @param relTypeUuid - is optional - if not provided, the default value of IS_A_REL is used.
	 * @param time - if null, source concept time is used
	 */
	public static TtkRelationshipChronicle addRelationship(TtkConceptChronicle eConcept, UUID targetUuid, UUID relTypeUuid, UUID relCharacteristicUUID, Long time)
	{
			List<TtkRelationshipChronicle> relationships = eConcept.getRelationships();
			if (relationships == null)
			{
				relationships = new ArrayList<TtkRelationshipChronicle>();
				eConcept.setRelationships(relationships);
			}
			TtkRelationshipChronicle rel = new TtkRelationshipChronicle();
			rel.setRelGroup(0);
			rel.setCharacteristicUuid(relCharacteristicUUID == null ? definingCharacteristicStatedUuid_ : relCharacteristicUUID);
			rel.setPrimordialComponentUuid((UuidT5Generator.get(RelationshipCAB.relSpecNamespace, eConcept.getPrimordialUuid().toString() + relTypeUuid.toString()
					+ targetUuid.toString() + rel.getRelGroup()
					+ (rel.getCharacteristicUuid().equals(definingCharacteristicStatedUuid_) ? "" : rel.getCharacteristicUuid().toString()))));
			rel.setC1Uuid(eConcept.getPrimordialUuid());
			rel.setTypeUuid(relTypeUuid == null ? isARelUuid_ : relTypeUuid);
			rel.setC2Uuid(targetUuid);
			rel.setRefinabilityUuid(notRefinableUuid);
			setRevisionAttributes(rel, null, time == null ? eConcept.getConceptAttributes().getTime() : time.longValue());
			relationships.add(rel);
			return rel;
	}
	

	public static void main(String[] args) throws MojoExecutionException, MojoFailureException
	{
		GenerateMetadataEConcepts gmc = new GenerateMetadataEConcepts(new File("foo.jbin"), new String[] {"org.ihtsdo.otf.tcc.api.metadata.binding.DynamicSememe"},
				new ConceptSpec[0], false);
		gmc.execute();
	}

}