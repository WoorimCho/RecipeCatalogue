-- Phase 4: a machine-readable quantity alongside the free-text `quantity`.
--   amount + unit  ->  fed to the nutrition / calorie / portion calculators.
-- Both nullable: a line may carry only free text ("a pinch"), only the pair,
-- or neither. The service rejects an amount without a unit.

alter table recipe_ingredient
    add column amount double,
    add column unit   varchar(20);
